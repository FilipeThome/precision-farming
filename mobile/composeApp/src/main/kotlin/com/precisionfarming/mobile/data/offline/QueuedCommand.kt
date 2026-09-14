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
     * sync succeeds and before the next GET reflects the server.
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
        return if (serverOutranks(serverStatus, projected)) serverStatus else projected
    }
}

private fun serverOutranks(server: String, projected: String): Boolean {
    fun rank(value: String): Int = when (value.uppercase()) {
        "COMPLETED", "COMPLETING" -> 4
        "PAUSED" -> 3
        "IN_PROGRESS", "STARTING" -> 2
        "PLANNED" -> 1
        else -> 0
    }
    val serverRank = rank(server)
    val projectedRank = rank(projected)
    if (serverRank > projectedRank) return true
    if (serverRank < projectedRank) return false
    return server.equals(projected, ignoreCase = true)
}
