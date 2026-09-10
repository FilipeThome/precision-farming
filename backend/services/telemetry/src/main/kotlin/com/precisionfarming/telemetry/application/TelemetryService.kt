package com.precisionfarming.telemetry.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.common.QueryLimits
import com.precisionfarming.common.concurrency.VirtualJobs
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoMachineFarms
import com.precisionfarming.telemetry.infrastructure.TelemetryEntity
import com.precisionfarming.telemetry.infrastructure.TelemetryId
import com.precisionfarming.telemetry.infrastructure.TelemetryJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable
import kotlin.math.cos
import kotlin.math.sin

data class TelemetryPoint(
    val id: UUID, val machineId: UUID, val observedAt: Instant,
    val lat: Double, val lon: Double, val speedKmh: Double, val rpm: Double,
    val fuelPct: Double, val engineTempC: Double,
)
data class TrackPoint(val lat: Double, val lon: Double, val observedAt: Instant)

@Service
class TelemetryService(private val repo: TelemetryJpaRepository) {
    fun history(scope: AccessScope, machineId: UUID, from: Instant, to: Instant): List<TelemetryPoint> {
        DemoMachineFarms.requireMachineRead(scope, machineId)
        requireRange(from, to)
        return repo.findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(machineId, from, to).map { it.toDto() }
    }

    fun track(scope: AccessScope, machineId: UUID, from: Instant, to: Instant): List<TrackPoint> {
        DemoMachineFarms.requireMachineRead(scope, machineId)
        requireRange(from, to)
        return repo.findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(machineId, from, to)
            .map { TrackPoint(it.lat, it.lon, it.observedAt) }
    }

    fun metrics(scope: AccessScope, machineId: UUID, from: Instant, to: Instant): MachineMetricsDto {
        DemoMachineFarms.requireMachineRead(scope, machineId)
        requireRange(from, to)
        val rows = repo.findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(machineId, from, to)
        return TelemetryMetrics.aggregate(
            rows.map { TelemetrySample(it.observedAt, it.speedKmh, it.fuelPct) },
        )
    }

    private fun requireRange(from: Instant, to: Instant) {
        if (!to.isAfter(from) || Duration.between(from, to).toDays() > QueryLimits.MAX_TELEMETRY_DAYS) {
            throw DomainException("TELEMETRY_RANGE_EXCEEDED", "Range exceeds ${QueryLimits.MAX_TELEMETRY_DAYS} days")
        }
    }

    @Transactional
    fun seed() {
        val end = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val windowStart = end.minus(7, ChronoUnit.DAYS)
        val series = VirtualJobs.all(
            SEED_MACHINES.mapIndexed { idx, (key, pos) ->
                Callable { gapFill(key, pos, idx, windowStart, end) }
            },
        )
        series.flatten().chunked(BATCH).forEach { repo.saveAll(it) }
    }

    private fun gapFill(
        machineKey: String,
        pos: Pair<Double, Double>,
        idx: Int,
        windowStart: Instant,
        end: Instant,
    ): List<TelemetryEntity> {
        val machineId = DemoIds.uuid(machineKey)
        val last = repo.findTopByMachineIdOrderByObservedAtDesc(machineId)
        val start = if (last == null) {
            windowStart
        } else {
            val nextHour = last.observedAt.truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS)
            if (nextHour.isAfter(windowStart)) nextHour else windowStart
        }
        if (!start.isBefore(end)) return emptyList()
        val points = generateSeries(machineKey, pos, idx, start, end)
        val existing = repo.findAllById(points.map { TelemetryId(it.id, it.observedAt) }).map { it.id }.toSet()
        return points.filter { it.id !in existing }
    }

    private fun generateSeries(
        machineKey: String,
        pos: Pair<Double, Double>,
        idx: Int,
        start: Instant,
        end: Instant,
    ): List<TelemetryEntity> {
        val machineId = DemoIds.uuid(machineKey)
        val points = ArrayList<TelemetryEntity>(POINTS_PER_MACHINE)
        var t = start
        var n = 0
        while (t.isBefore(end)) {
            val hourOfDay = t.atZone(ZoneOffset.UTC).hour
            val operating = hourOfDay in 6..18 && (idx + hourOfDay) % 7 != 0
            val hour = t.epochSecond / 3600.0
            points.add(
                TelemetryEntity(
                    id = DemoIds.uuid("tel-$machineKey-${HOUR_KEY.format(t)}"),
                    machineId = machineId,
                    observedAt = t,
                    lat = pos.first + 0.01 * sin(hour + idx),
                    lon = pos.second + 0.01 * cos(hour + idx),
                    speedKmh = if (operating) 8.0 + (n % 5) else 0.0,
                    rpm = if (operating) 1800.0 + (n % 80) else 700.0,
                    fuelPct = (85.0 - (n % 40)).coerceAtLeast(12.0),
                    engineTempC = if (operating) 88.0 + (n % 6) else 72.0,
                ),
            )
            n++
            t = t.plus(1, ChronoUnit.HOURS)
        }
        return points
    }

    private fun TelemetryEntity.toDto() =
        TelemetryPoint(id, machineId, observedAt, lat, lon, speedKmh, rpm, fuelPct, engineTempC)

    private companion object {
        const val BATCH = 500
        const val POINTS_PER_MACHINE = 7 * 24
        val HOUR_KEY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC)
        val SEED_MACHINES = listOf(
            "machine-001" to Pair(-19.39, -54.57),
            "machine-002" to Pair(-19.41, -54.55),
            "machine-003" to Pair(-19.37, -54.59),
            "machine-004" to Pair(-17.79, -50.92),
            "machine-005" to Pair(-12.54, -55.47),
            "machine-006" to Pair(-17.81, -50.90),
            "machine-007" to Pair(-13.05, -55.90),
            "machine-008" to Pair(-13.07, -55.92),
            "machine-009" to Pair(-22.22, -54.80),
            "machine-010" to Pair(-13.68, -57.88),
            "machine-011" to Pair(-16.62, -54.10),
            "machine-012" to Pair(-18.79, -52.62),
            "machine-013" to Pair(-12.54, -55.47),
            "machine-014" to Pair(-13.70, -57.86),
        )
    }
}

@Service
class TelemetrySeed(
    private val svc: TelemetryService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedTelemetry() = ApplicationRunner { if (seed) svc.seed() }
}
