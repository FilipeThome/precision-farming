package com.precisionfarming.mobile.data

import com.precisionfarming.mobile.data.offline.QueueState

fun OperationDto.canStart(): Boolean =
    status.equals("PLANNED", ignoreCase = true) || status.equals("PAUSED", ignoreCase = true)

/** Apply the latest SYNCED command so gates stay correct until the next GET. */
fun OperationDto.withQueuedStatus(queue: QueueState): OperationDto {
    val projected = queue.projectedStatus(id, status)
    return if (projected.equals(status, ignoreCase = true)) this else copy(status = projected)
}

fun OperationDto.canPause(): Boolean = status.equals("IN_PROGRESS", ignoreCase = true)

fun OperationDto.canComplete(): Boolean =
    status.equals("IN_PROGRESS", ignoreCase = true) || status.equals("PAUSED", ignoreCase = true)

fun AlertDto.isOpen(): Boolean = status.equals("OPEN", ignoreCase = true)

fun machineFreshness(lastObservedAt: String?): String? {
    if (lastObservedAt.isNullOrBlank()) return null
    val ageMs = runCatching {
        java.time.Instant.now().toEpochMilli() - java.time.Instant.parse(lastObservedAt).toEpochMilli()
    }.getOrNull() ?: return null
    return if (ageMs < 2 * 3_600_000) "LIVE" else "STALE"
}
