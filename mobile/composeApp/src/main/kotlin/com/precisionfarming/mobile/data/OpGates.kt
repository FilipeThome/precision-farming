package com.precisionfarming.mobile.data

fun OperationDto.canStart(): Boolean =
    status.equals("PLANNED", ignoreCase = true) || status.equals("PAUSED", ignoreCase = true)

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
