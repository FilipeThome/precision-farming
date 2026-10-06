package com.precisionfarming.mobile.data

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.round

fun rollingWeekIsoRange(): Pair<String, String> {
    val to = Instant.now()
    val from = to.minus(7, ChronoUnit.DAYS)
    return from.toString() to to.toString()
}

fun formatWhen(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return iso.replace('T', ' ').take(16)
}

fun formatNumber(value: Double?, digits: Int = 1): String {
    if (value == null || value.isNaN()) return "—"
    val factor = when (digits) {
        0 -> 1.0
        1 -> 10.0
        else -> 100.0
    }
    val rounded = round(value * factor) / factor
    return if (digits == 0) rounded.toInt().toString() else rounded.toString()
}
