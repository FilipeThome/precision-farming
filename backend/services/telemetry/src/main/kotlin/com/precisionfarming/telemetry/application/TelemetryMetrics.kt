package com.precisionfarming.telemetry.application

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class TelemetrySample(
    val observedAt: Instant,
    val speedKmh: Double,
    val fuelPct: Double,
)

data class TelemetryDayRow(
    val day: String,
    val hours: Double,
    val speed: Double,
    val fuel: Double,
)

data class MachineMetricsDto(
    val engineHours: Double,
    val lastObservedAt: Instant?,
    val days: List<TelemetryDayRow>,
)

object TelemetryMetrics {
    private const val MAX_DELTA_HOURS = 6.0
    private val DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)

    /**
     * Engine hours are Δt between consecutive samples when the previous point is moving
     * (`speedKmh > 0`), capped at [MAX_DELTA_HOURS]. The last moving sample also contributes
     * `min(1h, inferred step)` so hourly series do not drop the final interval.
     * Inferred step is the gap between the last two points, or 1h when only one point exists.
     */
    fun aggregate(points: List<TelemetrySample>): MachineMetricsDto {
        if (points.isEmpty()) return MachineMetricsDto(0.0, null, emptyList())
        val sorted = points.sortedBy { it.observedAt }
        var hours = 0.0
        val hourBuckets = linkedMapOf<String, Double>()
        val speedBuckets = linkedMapOf<String, Pair<Double, Int>>()
        val fuelBuckets = linkedMapOf<String, Pair<Double, Int>>()

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val dt = (sorted[i].observedAt.epochSecond - prev.observedAt.epochSecond) / 3600.0
            if (prev.speedKmh > 0 && dt > 0 && dt <= MAX_DELTA_HOURS) {
                hours += dt
                val day = DAY.format(prev.observedAt)
                hourBuckets[day] = (hourBuckets[day] ?: 0.0) + dt
            }
        }
        val last = sorted.last()
        if (last.speedKmh > 0) {
            val typicalStep = if (sorted.size >= 2) {
                val dt = (last.observedAt.epochSecond - sorted[sorted.size - 2].observedAt.epochSecond) / 3600.0
                if (dt > 0) kotlin.math.min(1.0, dt) else 1.0
            } else {
                1.0
            }
            hours += typicalStep
            val day = DAY.format(last.observedAt)
            hourBuckets[day] = (hourBuckets[day] ?: 0.0) + typicalStep
        }
        for (point in sorted) {
            val day = DAY.format(point.observedAt)
            if (point.speedKmh > 0) {
                val cur = speedBuckets[day] ?: (0.0 to 0)
                speedBuckets[day] = (cur.first + point.speedKmh) to (cur.second + 1)
            }
            val fuel = fuelBuckets[day] ?: (0.0 to 0)
            fuelBuckets[day] = (fuel.first + point.fuelPct) to (fuel.second + 1)
        }
        val days = (hourBuckets.keys + speedBuckets.keys + fuelBuckets.keys).toSortedSet().map { day ->
            val speed = speedBuckets[day]
            val fuel = fuelBuckets[day]
            TelemetryDayRow(
                day = day,
                hours = round1(hourBuckets[day] ?: 0.0),
                speed = if (speed == null || speed.second == 0) 0.0 else round1(speed.first / speed.second),
                fuel = if (fuel == null || fuel.second == 0) 0.0 else round1(fuel.first / fuel.second),
            )
        }
        return MachineMetricsDto(round1(hours), sorted.last().observedAt, days)
    }

    private fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0
}
