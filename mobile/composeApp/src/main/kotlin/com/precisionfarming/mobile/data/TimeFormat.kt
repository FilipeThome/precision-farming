package com.precisionfarming.mobile.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pure time helpers shared by Today / execution / sync screens. Locale-neutral output ("4 min", "2h", "3d"). */
object TimeFormat {
    private val clock = DateTimeFormatter.ofPattern("HH:mm")
    private val clockSeconds = DateTimeFormatter.ofPattern("HH:mm:ss")

    /** Tolerant ISO parser: Instant, offset date-time, or local date-time (interpreted in [zone]). */
    fun parseInstant(iso: String?, zone: ZoneId = ZoneId.systemDefault()): Instant? {
        if (iso.isNullOrBlank()) return null
        runCatching { return Instant.parse(iso) }
        runCatching { return OffsetDateTime.parse(iso).toInstant() }
        runCatching { return LocalDateTime.parse(iso).atZone(zone).toInstant() }
        runCatching { return LocalDate.parse(iso).atStartOfDay(zone).toInstant() }
        return null
    }

    /** Age of [iso] relative to [now]: "0 min", "4 min", "2h", "36h", "3d". Null when unparsable or in the future. */
    fun formatAge(iso: String?, now: Instant): String? {
        val at = parseInstant(iso) ?: return null
        val d = Duration.between(at, now)
        if (d.isNegative) return null
        val minutes = d.toMinutes()
        return when {
            minutes < 60 -> "$minutes min"
            d.toHours() < 48 -> "${d.toHours()}h"
            else -> "${d.toDays()}d"
        }
    }

    /** True when [iso] is older than [staleAfter] relative to [now]. Unparsable → false (caller hides the chip). */
    fun isStale(iso: String?, now: Instant, staleAfter: Duration = Duration.ofHours(24)): Boolean {
        val at = parseInstant(iso) ?: return false
        return Duration.between(at, now) > staleAfter
    }

    /** Elapsed operation time as HH:MM:SS (hours may exceed 24). Negative durations clamp to zero. */
    fun formatElapsed(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return "%02d:%02d:%02d".format(h, m, sec)
    }

    /** Remaining window as "1h 13min" / "45 min" / "2d 3h". Null when already past. */
    fun formatRemaining(until: Instant, now: Instant): String? {
        val d = Duration.between(now, until)
        if (d.isNegative || d.isZero) return null
        val days = d.toDays()
        val hours = d.toHours() % 24
        val minutes = d.toMinutes() % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}min"
            else -> "$minutes min"
        }
    }

    fun clock(iso: String?, zone: ZoneId): String? =
        parseInstant(iso, zone)?.let { LocalTime.ofInstant(it, zone).format(clock) }

    fun clockWithSeconds(iso: String?, zone: ZoneId): String? =
        parseInstant(iso, zone)?.let { LocalTime.ofInstant(it, zone).format(clockSeconds) }

    /** "06:00 – 10:30", "06:00 –" or "– 10:30"; null when both are missing. */
    fun window(startIso: String?, endIso: String?, zone: ZoneId): String? {
        val start = clock(startIso, zone)
        val end = clock(endIso, zone)
        return when {
            start != null && end != null -> "$start – $end"
            start != null -> "$start –"
            end != null -> "– $end"
            else -> null
        }
    }
}
