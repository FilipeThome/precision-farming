package com.precisionfarming.mobile.data.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class FlushResult(val synced: Int, val failed: Int, val retried: Int) {
    val attempted: Int get() = synced + failed + retried
}

/**
 * Pure-Kotlin offline command queue. State machine:
 * PENDING → SYNCING → SYNCED (2xx) | FAILED (4xx, never auto-retried) | PENDING+attempts (IO/5xx, backoff).
 * Replay stops at the first transient failure so ordering per device is preserved.
 */
class OfflineQueue(
    private val store: QueueStore,
    private val executor: CommandExecutor,
    private val clock: () -> Instant = { Instant.now() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val backoff: (attempts: Int) -> Duration = ::defaultBackoff,
    private val historyLimit: Int = DEFAULT_HISTORY,
) {
    private val _state: MutableStateFlow<QueueState>
    val state: StateFlow<QueueState>
    private val flushLock = Mutex()

    init {
        val raw = store.load()
        val recovered = recoverStuck(raw)
        _state = MutableStateFlow(recovered)
        state = _state.asStateFlow()
        if (recovered != raw) store.save(recovered)
    }

    /**
     * Single-flight per operation: a second command is refused while one is still PENDING or SYNCING.
     * The existing open command is returned unchanged.
     */
    fun enqueue(command: OpCommand, userId: String? = null): QueuedCommand {
        var returned: QueuedCommand? = null
        mutate { state ->
            val open = state.openFor(command.operationId)
            if (open != null) {
                returned = open
                return@mutate state
            }
            val queued = QueuedCommand(
                clientOperationId = idGenerator(),
                operationId = command.operationId,
                type = command.type,
                reason = command.reason,
                createdAt = clock().toString(),
                userId = userId,
                fromStatus = command.fromStatus,
                actualLiters = command.actualLiters,
            )
            returned = queued
            state.copy(items = state.items + queued)
        }
        return returned ?: error("enqueue did not produce a command")
    }

    /** Drop a FAILED item (user acknowledged the rejection). */
    fun dismiss(clientOperationId: String) {
        mutate { s -> s.copy(items = s.items.filterNot { it.clientOperationId == clientOperationId && it.state == SyncState.FAILED }) }
    }

    /** Called after a successful `/sync/pull` so the strip can show a real last-sync time. */
    fun recordSync(at: Instant = clock()) {
        mutate { it.copy(lastSyncAt = at.toString()) }
    }

    /** Logout / user switch: nothing on disk for the next session. */
    fun clear() {
        mutate { QueueState() }
    }

    /** Keep only commands captured by [userId]. Unattributed leftovers are dropped, never replayed. */
    fun retainForUser(userId: String) {
        mutate { s -> s.copy(items = s.items.filter { it.userId == userId }) }
    }

    /**
     * Replays open commands in creation order. [force] ignores the backoff schedule (manual "Sync now").
     * When [userId] is set, only that user's commands are replayed.
     * Serialised: concurrent callers wait for the running flush and then re-scan.
     */
    suspend fun flush(force: Boolean = false, userId: String? = null): FlushResult = flushLock.withLock {
        var synced = 0
        var failed = 0
        var retried = 0
        while (true) {
            val now = clock()
            val next = _state.value.items
                .filter { it.state == SyncState.PENDING }
                .filter { userId == null || it.userId == userId }
                .minByOrNull { it.createdAt }
                ?: break
            val waitUntil = OfflineQueue.parseInstant(next.nextAttemptAt)
            if (!force && waitUntil != null && waitUntil.isAfter(now)) break
            setState(next.clientOperationId) { it.copy(state = SyncState.SYNCING) }
            val result = executor.execute(next.copy(state = SyncState.SYNCING))
            val at = clock()
            when (result) {
                is ExecResult.Ok -> {
                    synced++
                    mutate { s ->
                        s.copy(
                            items = trimHistory(
                                s.items.map {
                                    if (it.clientOperationId == next.clientOperationId) {
                                        it.copy(state = SyncState.SYNCED, syncedAt = at.toString(), lastError = null, nextAttemptAt = null)
                                    } else it
                                },
                            ),
                            lastSyncAt = at.toString(),
                        )
                    }
                }
                is ExecResult.Rejected -> {
                    failed++
                    setState(next.clientOperationId) {
                        it.copy(state = SyncState.FAILED, attempts = it.attempts + 1, lastError = result.message, nextAttemptAt = null)
                    }
                }
                is ExecResult.Retry -> {
                    retried++
                    setState(next.clientOperationId) {
                        val attempts = it.attempts + 1
                        it.copy(
                            state = SyncState.PENDING,
                            attempts = attempts,
                            lastError = result.message,
                            nextAttemptAt = at.plus(backoff(attempts)).toString(),
                        )
                    }
                    break
                }
            }
        }
        FlushResult(synced, failed, retried)
    }

    private fun setState(clientOperationId: String, transform: (QueuedCommand) -> QueuedCommand) {
        mutate { s -> s.copy(items = s.items.map { if (it.clientOperationId == clientOperationId) transform(it) else it }) }
    }

    private fun trimHistory(items: List<QueuedCommand>): List<QueuedCommand> {
        val synced = items.filter { it.state == SyncState.SYNCED }
        if (synced.size <= historyLimit) return items
        val drop = synced.sortedBy { it.syncedAt ?: it.createdAt }.take(synced.size - historyLimit).map { it.clientOperationId }.toSet()
        return items.filterNot { it.clientOperationId in drop }
    }

    private fun mutate(transform: (QueueState) -> QueueState) {
        _state.update(transform)
        store.save(_state.value)
    }

    companion object {
        const val DEFAULT_HISTORY = 20

        /** 5s, 10s, 20s … capped at 5 min. */
        fun defaultBackoff(attempts: Int): Duration {
            val seconds = (5L shl (attempts - 1).coerceIn(0, 6)).coerceAtMost(300L)
            return Duration.ofSeconds(seconds)
        }

        /** Crash during flush can leave SYNCING on disk; those must be retryable. */
        fun recoverStuck(state: QueueState): QueueState {
            if (state.items.none { it.state == SyncState.SYNCING }) return state
            return state.copy(items = state.items.map { if (it.state == SyncState.SYNCING) it.copy(state = SyncState.PENDING) else it })
        }

        fun parseInstant(raw: String?): Instant? =
            raw?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }
}
