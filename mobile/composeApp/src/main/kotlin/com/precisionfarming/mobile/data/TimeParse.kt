package com.precisionfarming.mobile.data

import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

/** Parse ISO-8601 Instant or OffsetDateTime strings to epoch millis. */
fun parseEpochMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
}

/** Locale-stable decimal for scores / tons (always `.` separator). */
fun formatDecimal(value: Double, fractionDigits: Int = 2): String =
    String.format(Locale.US, "%.${fractionDigits}f", value)

fun formatTons(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        formatDecimal(rounded, 1)
    }
}
