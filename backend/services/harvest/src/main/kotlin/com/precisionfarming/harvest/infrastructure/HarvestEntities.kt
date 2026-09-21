package com.precisionfarming.harvest.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "harvest_plans")
class HarvestPlanEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID,
    val crop: String,
    @Column(name = "planned_start") val plannedStart: Instant,
    @Column(name = "planned_end") val plannedEnd: Instant,
    @Column(name = "expected_t_ha") val expectedTHa: BigDecimal,
    var status: String,
)

@Entity
@Table(name = "harvest_yields")
class HarvestYieldEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID,
    @Column(name = "plan_id") val planId: UUID?,
    @Column(name = "recorded_at") val recordedAt: Instant,
    @Column(name = "yield_t_ha") val yieldTHa: BigDecimal,
    @Column(name = "moisture_pct") val moisturePct: BigDecimal?,
    @Column(name = "area_ha") val areaHa: BigDecimal?,
)

@Entity
@Table(name = "logistics_loads")
class LogisticsLoadEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "plan_id") val planId: UUID?,
    @Column(name = "truck_plate") val truckPlate: String,
    val destination: String,
    val tons: BigDecimal,
    var status: String,
    @Column(name = "dispatched_at") var dispatchedAt: Instant?,
)

@Entity
@Table(name = "storage_units")
class StorageUnitEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") var farmId: UUID,
    var name: String,
    @Column(name = "capacity_t") var capacityT: BigDecimal,
    @Column(name = "used_t") var usedT: BigDecimal,
    var type: String,
)

@Entity
@Table(name = "storage_lots")
class StorageLotEntity(
    @Id val id: UUID,
    @Column(name = "unit_id") val unitId: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val crop: String,
    val tons: BigDecimal,
    val quality: String,
    @Column(name = "received_at") val receivedAt: Instant,
)

interface HarvestPlanJpaRepository : JpaRepository<HarvestPlanEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<HarvestPlanEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<HarvestPlanEntity>
}
interface HarvestYieldJpaRepository : JpaRepository<HarvestYieldEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<HarvestYieldEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<HarvestYieldEntity>
}
interface LogisticsLoadJpaRepository : JpaRepository<LogisticsLoadEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<LogisticsLoadEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<LogisticsLoadEntity>
}
interface StorageUnitJpaRepository : JpaRepository<StorageUnitEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<StorageUnitEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<StorageUnitEntity>
}
interface StorageLotJpaRepository : JpaRepository<StorageLotEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<StorageLotEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<StorageLotEntity>
}
