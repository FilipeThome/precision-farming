package com.precisionfarming.mobile.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayRange(val start: Long, val end: Long)

data class Segment(val leftPct: Double, val widthPct: Double)

data class TimelineBand(val rating: String, val leftPct: Double, val widthPct: Double)

data class TimelineRow(
    val operation: OperationDto,
    val planned: Segment? = null,
    val executed: Segment? = null,
)

/** Calendar day containing [now] in [zone] (00:00 → next 00:00). */
fun todayRange(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DayRange {
    val start = LocalDate.ofInstant(now, zone).atStartOfDay(zone).toInstant().toEpochMilli()
    return DayRange(start = start, end = start + 24L * 60 * 60 * 1000)
}

private fun intersects(start: Long?, end: Long?, range: DayRange): Boolean {
    if (start == null && end == null) return false
    val s = start ?: end!!
    val e = end ?: start!!
    return s < range.end && e > range.start
}

/** Operations whose planned window (or actual execution) intersects the day. */
fun todayOperations(operations: List<OperationDto>, range: DayRange): List<OperationDto> =
    operations
        .filter {
            intersects(parseEpochMillis(it.plannedStart), parseEpochMillis(it.plannedEnd), range) ||
                intersects(parseEpochMillis(it.actualStart), parseEpochMillis(it.actualEnd), range)
        }
        .sortedBy { it.plannedStart ?: it.actualStart ?: "" }

/** Clamped percentage segment of [start,end] within the range. */
fun toSegment(start: Long?, end: Long?, range: DayRange): Segment? {
    if (start == null && end == null) return null
    val total = (range.end - range.start).toDouble()
    val s = maxOf(range.start, start ?: range.start)
    val e = minOf(range.end, end ?: range.end)
    if (e <= s) return null
    return Segment(
        leftPct = roundPct(((s - range.start) / total) * 100),
        widthPct = roundPct(((e - s) / total) * 100),
    )
}

private fun roundPct(value: Double): Double = kotlin.math.round(value * 100) / 100.0

fun timelineRows(
    operations: List<OperationDto>,
    range: DayRange,
    now: Long = System.currentTimeMillis(),
): List<TimelineRow> =
    todayOperations(operations, range).map { operation ->
        TimelineRow(
            operation = operation,
            planned = toSegment(parseEpochMillis(operation.plannedStart), parseEpochMillis(operation.plannedEnd), range),
            executed = if (operation.actualStart != null) {
                toSegment(
                    parseEpochMillis(operation.actualStart),
                    parseEpochMillis(operation.actualEnd) ?: minOf(now, range.end),
                    range,
                )
            } else {
                null
            },
        )
    }

/** Compact list rows for Tower UI (ops intersecting today). */
fun compactTodayTimeline(
    operations: List<OperationDto>,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<TimelineRow> = timelineRows(operations, todayRange(now, zone), now.toEpochMilli())
