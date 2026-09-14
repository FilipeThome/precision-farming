package com.precisionfarming.mobile.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayKpis(val opsToday: Int, val completedToday: Int, val openAlerts: Int) {
    /** "1/4" or null when there is nothing planned today (UI shows "—"). */
    val completedLabel: String? get() = if (opsToday == 0) null else "$completedToday/$opsToday"
}

/** Pure "Today" logic. All time reasoning is injected (`now`, `zone`) so it is unit-testable. */
object TodayOps {
    /** `FarmDto.timezone` when exactly one farm is selected and it has one; otherwise the system default. */
    fun resolveZone(farms: List<FarmDto>, selectedFarmId: String?, fallback: ZoneId = ZoneId.systemDefault()): ZoneId {
        if (selectedFarmId.isNullOrBlank()) return fallback
        val tz = farms.firstOrNull { it.id == selectedFarmId }?.timezone ?: return fallback
        return runCatching { ZoneId.of(tz) }.getOrDefault(fallback)
    }

    /** Operations whose planned window intersects the local day of [now] in [zone], sorted by plannedStart. */
    fun operationsToday(ops: List<OperationDto>, now: Instant, zone: ZoneId): List<OperationDto> {
        val today = LocalDate.ofInstant(now, zone)
        val dayStart = today.atStartOfDay(zone).toInstant()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
        return ops
            .filter { op ->
                val start = TimeFormat.parseInstant(op.plannedStart, zone)
                val end = TimeFormat.parseInstant(op.plannedEnd, zone)
                when {
                    start != null && end != null -> start.isBefore(dayEnd) && end.isAfter(dayStart)
                    start != null -> !start.isBefore(dayStart) && start.isBefore(dayEnd)
                    end != null -> !end.isBefore(dayStart) && end.isBefore(dayEnd)
                    else -> false
                }
            }
            .sortedWith(compareBy(nullsLast<Instant>()) { TimeFormat.parseInstant(it.plannedStart, zone) })
    }

    /**
     * Next operation the operator should act on: any IN_PROGRESS first (earliest actualStart),
     * then the earliest PLANNED/PAUSED by plannedStart (unscheduled last). Null when nothing is actionable.
     */
    fun nextActionable(ops: List<OperationDto>, zone: ZoneId): OperationDto? {
        val running = ops
            .filter { it.canPause() }
            .minWithOrNull(compareBy(nullsLast<Instant>()) { TimeFormat.parseInstant(it.actualStart ?: it.plannedStart, zone) })
        if (running != null) return running
        return ops
            .filter { it.canStart() }
            .minWithOrNull(compareBy(nullsLast<Instant>()) { TimeFormat.parseInstant(it.plannedStart, zone) })
    }

    fun todayKpis(ops: List<OperationDto>, alerts: List<AlertDto>, now: Instant, zone: ZoneId): TodayKpis {
        val today = operationsToday(ops, now, zone)
        return TodayKpis(
            opsToday = today.size,
            completedToday = today.count { it.status.equals("COMPLETED", ignoreCase = true) },
            openAlerts = alerts.count { it.isOpen() },
        )
    }

    /** Weather window rating (FAVORABLE/MARGINAL/UNFAVORABLE) intersecting the planned window, if any. */
    fun weatherRatingFor(op: OperationDto, windows: List<WeatherWindowDto>, zone: ZoneId): String? {
        val start = TimeFormat.parseInstant(op.plannedStart, zone) ?: return null
        val end = TimeFormat.parseInstant(op.plannedEnd, zone) ?: start
        return windows.firstOrNull { w ->
            val ws = TimeFormat.parseInstant(w.startAt, zone) ?: return@firstOrNull false
            val we = TimeFormat.parseInstant(w.endAt, zone) ?: ws
            (op.farmId == null || w.farmId == null || w.farmId == op.farmId) &&
                ws.isBefore(end.plusSeconds(1)) && we.isAfter(start.minusSeconds(1)) && !w.rating.isNullOrBlank()
        }?.rating
    }

    /** morning / afternoon / evening bucket for the greeting. */
    fun greetingKey(now: Instant, zone: ZoneId): String {
        val hour = now.atZone(zone).hour
        return when {
            hour < 12 -> "today.greeting.morning"
            hour < 18 -> "today.greeting.afternoon"
            else -> "today.greeting.evening"
        }
    }
}
