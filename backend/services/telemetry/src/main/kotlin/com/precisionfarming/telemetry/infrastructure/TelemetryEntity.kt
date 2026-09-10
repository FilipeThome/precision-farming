package com.precisionfarming.telemetry.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class TelemetryId(val id: UUID = UUID.randomUUID(), val observedAt: Instant = Instant.now()) : Serializable

@Entity
@Table(name = "telemetry_observations")
@IdClass(TelemetryId::class)
class TelemetryEntity(
    @Id val id: UUID,
    @Column(name = "machine_id") val machineId: UUID,
    @Id @Column(name = "observed_at") val observedAt: Instant,
    val lat: Double,
    val lon: Double,
    @Column(name = "speed_kmh") val speedKmh: Double,
    val rpm: Double,
    @Column(name = "fuel_pct") val fuelPct: Double,
    @Column(name = "engine_temp_c") val engineTempC: Double,
)

interface TelemetryJpaRepository : JpaRepository<TelemetryEntity, TelemetryId> {
    fun existsByMachineId(machineId: UUID): Boolean
    fun findTopByMachineIdOrderByObservedAtDesc(machineId: UUID): TelemetryEntity?
    fun findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(
        machineId: UUID,
        from: Instant,
        to: Instant,
    ): List<TelemetryEntity>
}
