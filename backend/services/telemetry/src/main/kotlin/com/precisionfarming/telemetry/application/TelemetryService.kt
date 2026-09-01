package com.precisionfarming.telemetry.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.common.QueryLimits
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoMachineFarms
import com.precisionfarming.common.concurrency.VirtualJobs
import com.precisionfarming.telemetry.infrastructure.TelemetryEntity
import com.precisionfarming.telemetry.infrastructure.TelemetryJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
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
        DemoMachineFarms.requireMachine(scope, machineId)
        requireRange(from, to)
        return repo.findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(machineId, from, to).map { it.toDto() }
    }

    fun track(scope: AccessScope, machineId: UUID, from: Instant, to: Instant): List<TrackPoint> {
        DemoMachineFarms.requireMachine(scope, machineId)
        requireRange(from, to)
        return repo.findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(machineId, from, to)
            .map { TrackPoint(it.lat, it.lon, it.observedAt) }
    }

    private fun requireRange(from: Instant, to: Instant) {
        if (!to.isAfter(from) || Duration.between(from, to).toDays() > QueryLimits.MAX_TELEMETRY_DAYS) {
            throw DomainException("TELEMETRY_RANGE_EXCEEDED", "Range exceeds ${QueryLimits.MAX_TELEMETRY_DAYS} days")
        }
    }

    @Transactional
    fun seed() {
        val machines = listOf("machine-001" to Pair(-19.39, -54.57), "machine-002" to Pair(-19.41, -54.55))
        if (repo.existsByMachineId(DemoIds.uuid(machines.first().first))) return
        val end = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val start = end.minus(7, ChronoUnit.DAYS)
        val series = VirtualJobs.all(
            machines.mapIndexed { idx, (key, pos) ->
                Callable { generateSeries(DemoIds.uuid(key), pos, idx, start, end) }
            },
        )
        series.flatten().chunked(BATCH).forEach { repo.saveAll(it) }
    }

    private fun generateSeries(
        machineId: UUID,
        pos: Pair<Double, Double>,
        idx: Int,
        start: Instant,
        end: Instant,
    ): List<TelemetryEntity> {
        val points = ArrayList<TelemetryEntity>(POINTS_PER_MACHINE)
        var t = start
        var n = 0
        while (t.isBefore(end)) {
            val hour = t.epochSecond / 3600.0
            val operating = (t.epochSecond / 60) % 180 < 120
            points.add(
                TelemetryEntity(
                    id = UUID.randomUUID(),
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
            t = t.plus(15, ChronoUnit.MINUTES)
        }
        return points
    }

    private fun TelemetryEntity.toDto() =
        TelemetryPoint(id, machineId, observedAt, lat, lon, speedKmh, rpm, fuelPct, engineTempC)

    private companion object {
        const val BATCH = 500
        const val POINTS_PER_MACHINE = 7 * 24 * 4
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
