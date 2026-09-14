package com.precisionfarming.mobile.data.offline

import kotlinx.serialization.Serializable

@Serializable
enum class SyncState { PENDING, SYNCING, SYNCED, FAILED }

/**
 * One idempotent command captured on the device. [clientOperationId] is generated once at enqueue
 * time and reused on every replay so the backend can de-duplicate.
 */
@Serializable
data class QueuedCommand(
    val clientOperationId: String,
    val operationId: String,
    val type: OpCommandType,
    val reason: String? = null,
    val createdAt: String,
    val state: SyncState = SyncState.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val syncedAt: String? = null,
    /** ISO instant before which the queue will not auto-retry (backoff). Null = ready now. */
    val nextAttemptAt: String? = null,
    /** Session that captured the command. Replay is refused for any other user. */
    val userId: String? = null,
    /** Status the command was issued from, so projection can tell a stale GET from a later move. */
    val fromStatus: String? = null,
) {
    val isOpen: Boolean get() = state == SyncState.PENDING || state == SyncState.SYNCING
}

@Serializable
data class QueueState(
    val items: List<QueuedCommand> = emptyList(),
    /** ISO instant of the last successful sync (command synced or pull completed). Null until one exists. */
    val lastSyncAt: String? = null,
) {
    val pendingCount: Int get() = items.count { it.isOpen }
    val failedCount: Int get() = items.count { it.state == SyncState.FAILED }

    /** Open (pending/syncing) command for [operationId], oldest first, or null. */
    fun openFor(operationId: String): QueuedCommand? =
        items.firstOrNull { it.operationId == operationId && it.isOpen }

    fun failedFor(operationId: String): List<QueuedCommand> =
        items.filter { it.operationId == operationId && it.state == SyncState.FAILED }

    /**
     * Status implied by the latest SYNCED command. Used so Start/Pause/Complete stay gated after
     * sync succeeds and before the next GET reflects the server. A later server status that is not
     * the command's source state still wins (another operator paused after a start from PLANNED).
     */
    fun projectedStatus(operationId: String, serverStatus: String): String {
        val lastSynced = items
            .filter { it.operationId == operationId && it.state == SyncState.SYNCED }
            .maxByOrNull { it.syncedAt ?: it.createdAt }
            ?: return serverStatus
        val projected = when (lastSynced.type) {
            OpCommandType.START -> "IN_PROGRESS"
            OpCommandType.PAUSE -> "PAUSED"
            OpCommandType.COMPLETE -> "COMPLETED"
        }
        val server = serverStatus.uppercase()
        if (server == projected.uppercase() || server in setOf("COMPLETED", "COMPLETING")) return serverStatus
        val stale = staleSources(lastSynced.type, lastSynced.fromStatus)
        return if (server in stale) projected else serverStatus
    }
}

private fun staleSources(type: OpCommandType, fromStatus: String?): Set<String> {
    val from = fromStatus?.uppercase()
    return when (type) {
        OpCommandType.START -> when (from) {
            "PAUSED" -> setOf("PAUSED", "STARTING")
            "PLANNED" -> setOf("PLANNED", "STARTING")
            else -> setOf("PLANNED", "PAUSED", "STARTING")
        }
        OpCommandType.PAUSE -> setOf("IN_PROGRESS", "STARTING")
        OpCommandType.COMPLETE -> setOf("IN_PROGRESS", "PAUSED", "COMPLETING")
    }
}
