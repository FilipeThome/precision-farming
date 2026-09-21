package com.precisionfarming.harvest.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DemoCatalog
import com.precisionfarming.common.DomainException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.FieldFarmGuard
import com.precisionfarming.harvest.domain.HarvestPlanStatus
import com.precisionfarming.harvest.domain.LoadStatus
import com.precisionfarming.harvest.infrastructure.HarvestPlanEntity
import com.precisionfarming.harvest.infrastructure.HarvestPlanJpaRepository
import com.precisionfarming.harvest.infrastructure.HarvestYieldEntity
import com.precisionfarming.harvest.infrastructure.HarvestYieldJpaRepository
import com.precisionfarming.harvest.infrastructure.LogisticsLoadEntity
import com.precisionfarming.harvest.infrastructure.LogisticsLoadJpaRepository
import com.precisionfarming.harvest.infrastructure.StorageLotEntity
import com.precisionfarming.harvest.infrastructure.StorageLotJpaRepository
import com.precisionfarming.harvest.infrastructure.StorageUnitEntity
import com.precisionfarming.harvest.infrastructure.StorageUnitJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class HarvestPlanDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID, val crop: String, val plannedStart: Instant,
    val plannedEnd: Instant, val expectedTHa: BigDecimal, val status: String,
)
data class CreateHarvestPlan(val farmId: UUID, val fieldId: UUID, val crop: String, val expectedTHa: BigDecimal)
data class HarvestYieldDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID, val fieldName: String?, val planId: UUID?, val recordedAt: Instant,
    val yieldTHa: BigDecimal, val moisturePct: BigDecimal?, val areaHa: BigDecimal?,
)
data class LogisticsLoadDto(
    val id: UUID, val farmId: UUID, val planId: UUID?, val truckPlate: String, val destination: String,
    val tons: BigDecimal, val status: String, val dispatchedAt: Instant?,
)
data class DispatchRequest(val loadId: UUID)
data class StorageUnitDto(val id: UUID, val farmId: UUID, val name: String, val capacityT: BigDecimal, val usedT: BigDecimal, val type: String)
data class UpsertStorageUnit(val farmId: UUID, val name: String, val type: String, val capacityT: BigDecimal, val usedT: BigDecimal)
data class StorageLotDto(val id: UUID, val unitId: UUID, val farmId: UUID, val crop: String, val tons: BigDecimal, val quality: String, val receivedAt: Instant)

@Service
class HarvestService(
    private val plans: HarvestPlanJpaRepository,
    private val yields: HarvestYieldJpaRepository,
    private val loads: LogisticsLoadJpaRepository,
    private val units: StorageUnitJpaRepository,
    private val lots: StorageLotJpaRepository,
    private val fieldFarms: FieldFarmGuard,
) {
    fun listPlans(scope: AccessScope, farmId: UUID?) =
        plans.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createPlan(scope: AccessScope, cmd: CreateHarvestPlan): HarvestPlanDto {
        scope.requireFarm(cmd.farmId)
        fieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        val now = Instant.now()
        return plans.save(
            HarvestPlanEntity(
                UUID.randomUUID(), cmd.farmId, cmd.fieldId, cmd.crop,
                now.plus(1, ChronoUnit.DAYS), now.plus(5, ChronoUnit.DAYS), cmd.expectedTHa, HarvestPlanStatus.PLANNED.name,
            ),
        ).toDto()
    }

    fun listYield(scope: AccessScope, farmId: UUID?) =
        yields.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }
    fun listLoads(scope: AccessScope, farmId: UUID?) =
        loads.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun dispatch(scope: AccessScope, cmd: DispatchRequest): LogisticsLoadDto {
        val e = loads.findById(cmd.loadId).orElseThrow { NotFoundException("LOAD_NOT_FOUND", "Load not found") }
        scope.requireEntityFarm(e.farmId)
        e.status = LoadStatus.DISPATCHED.name
        e.dispatchedAt = Instant.now()
        return loads.save(e).toDto()
    }

    fun listUnits(scope: AccessScope, farmId: UUID?) =
        units.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createUnit(scope: AccessScope, cmd: UpsertStorageUnit): StorageUnitDto {
        scope.requireFarm(cmd.farmId)
        validateCapacity(cmd.capacityT, cmd.usedT)
        return units.save(
            StorageUnitEntity(UUID.randomUUID(), cmd.farmId, cmd.name, cmd.capacityT, cmd.usedT, cmd.type),
        ).toDto()
    }

    @Transactional
    fun patchUnit(scope: AccessScope, id: UUID, cmd: UpsertStorageUnit): StorageUnitDto {
        val e = units.findById(id).orElseThrow { NotFoundException("UNIT_NOT_FOUND", "Storage unit not found") }
        scope.requireEntityFarm(e.farmId)
        scope.requireFarm(cmd.farmId)
        validateCapacity(cmd.capacityT, cmd.usedT)
        val farmChanged = e.farmId != cmd.farmId
        e.farmId = cmd.farmId
        e.name = cmd.name
        e.type = cmd.type
        e.capacityT = cmd.capacityT
        e.usedT = cmd.usedT
        val saved = units.save(e)
        if (farmChanged) {
            val moved = lots.findByUnitId(id)
            moved.forEach { it.farmId = cmd.farmId }
            if (moved.isNotEmpty()) lots.saveAll(moved)
        }
        return saved.toDto()
    }
    fun listLots(scope: AccessScope, farmId: UUID?) =
        lots.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun seed() {
        val now = Instant.now()
        val planRows = listOf(
            HarvestPlanEntity(DemoIds.uuid("hplan-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "SOY", now.plus(2, ChronoUnit.DAYS), now.plus(6, ChronoUnit.DAYS), BigDecimal("3.8"), HarvestPlanStatus.PLANNED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "CORN", now.plus(10, ChronoUnit.DAYS), now.plus(16, ChronoUnit.DAYS), BigDecimal("8.2"), HarvestPlanStatus.PLANNED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-003"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-003"), "SOY", now.minus(5, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), BigDecimal("3.5"), HarvestPlanStatus.COMPLETED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-004"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "SOY", now.plus(3, ChronoUnit.DAYS), now.plus(8, ChronoUnit.DAYS), BigDecimal("3.9"), HarvestPlanStatus.PLANNED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-005"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-005"), "CORN", now.plus(20, ChronoUnit.DAYS), now.plus(28, ChronoUnit.DAYS), BigDecimal("7.8"), HarvestPlanStatus.PLANNED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-006"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "SOY", now.plus(4, ChronoUnit.DAYS), now.plus(9, ChronoUnit.DAYS), BigDecimal("4.1"), HarvestPlanStatus.IN_PROGRESS.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-007"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-007"), "CORN", now.plus(12, ChronoUnit.DAYS), now.plus(18, ChronoUnit.DAYS), BigDecimal("7.5"), HarvestPlanStatus.PLANNED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-008"), DemoIds.uuid("farm-004"), DemoIds.uuid("field-009"), "SOY", now.minus(2, ChronoUnit.DAYS), now.plus(3, ChronoUnit.DAYS), BigDecimal("3.7"), HarvestPlanStatus.IN_PROGRESS.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-009"), DemoIds.uuid("farm-005"), DemoIds.uuid("field-012"), "SOY", now.plus(7, ChronoUnit.DAYS), now.plus(14, ChronoUnit.DAYS), BigDecimal("3.6"), HarvestPlanStatus.PLANNED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-010"), DemoIds.uuid("farm-006"), DemoIds.uuid("field-017"), "SOY", now.plus(5, ChronoUnit.DAYS), now.plus(11, ChronoUnit.DAYS), BigDecimal("4.0"), HarvestPlanStatus.PLANNED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-011"), DemoIds.uuid("farm-007"), DemoIds.uuid("field-019"), "SOY", now.minus(8, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS), BigDecimal("3.4"), HarvestPlanStatus.COMPLETED.name),
            HarvestPlanEntity(DemoIds.uuid("hplan-012"), DemoIds.uuid("farm-008"), DemoIds.uuid("field-021"), "SOY", now.plus(9, ChronoUnit.DAYS), now.plus(15, ChronoUnit.DAYS), BigDecimal("3.55"), HarvestPlanStatus.PLANNED.name),
        )
        plans.saveAll(planRows)

        val yieldRows = listOf(
            HarvestYieldEntity(DemoIds.uuid("hyield-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-003"), DemoIds.uuid("hplan-003"), now.minus(1, ChronoUnit.DAYS), BigDecimal("3.62"), BigDecimal("13.5"), BigDecimal("80")),
            HarvestYieldEntity(DemoIds.uuid("hyield-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), null, now.minus(30, ChronoUnit.DAYS), BigDecimal("3.45"), BigDecimal("14.0"), BigDecimal("120")),
            HarvestYieldEntity(DemoIds.uuid("hyield-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), null, now.minus(40, ChronoUnit.DAYS), BigDecimal("3.90"), BigDecimal("12.8"), BigDecimal("210")),
            HarvestYieldEntity(DemoIds.uuid("hyield-004"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), DemoIds.uuid("hplan-006"), now.minus(2, ChronoUnit.DAYS), BigDecimal("4.05"), BigDecimal("13.1"), BigDecimal("300")),
            HarvestYieldEntity(DemoIds.uuid("hyield-005"), DemoIds.uuid("farm-004"), DemoIds.uuid("field-009"), DemoIds.uuid("hplan-008"), now.minus(1, ChronoUnit.DAYS), BigDecimal("3.68"), BigDecimal("13.8"), BigDecimal("140")),
            HarvestYieldEntity(DemoIds.uuid("hyield-006"), DemoIds.uuid("farm-005"), DemoIds.uuid("field-013"), null, now.minus(25, ChronoUnit.DAYS), BigDecimal("7.60"), BigDecimal("14.2"), BigDecimal("88")),
            HarvestYieldEntity(DemoIds.uuid("hyield-007"), DemoIds.uuid("farm-007"), DemoIds.uuid("field-019"), DemoIds.uuid("hplan-011"), now.minus(2, ChronoUnit.DAYS), BigDecimal("3.41"), BigDecimal("13.0"), BigDecimal("118")),
            HarvestYieldEntity(DemoIds.uuid("hyield-008"), DemoIds.uuid("farm-006"), DemoIds.uuid("field-018"), null, now.minus(35, ChronoUnit.DAYS), BigDecimal("7.20"), BigDecimal("14.5"), BigDecimal("130")),
        )
        yields.saveAll(yieldRows)

        val loadRows = listOf(
            LogisticsLoadEntity(DemoIds.uuid("load-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("hplan-003"), "MSX-1A23", "CENTRAL_WAREHOUSE", BigDecimal("32"), LoadStatus.QUEUED.name, null),
            LogisticsLoadEntity(DemoIds.uuid("load-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("hplan-003"), "MSX-2B44", "DRY_PORT", BigDecimal("28"), LoadStatus.DISPATCHED.name, now.minus(4, ChronoUnit.HOURS)),
            LogisticsLoadEntity(DemoIds.uuid("load-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("hplan-004"), "GOY-9C11", "SANTA_HELENA_SILO", BigDecimal("35"), LoadStatus.QUEUED.name, null),
            LogisticsLoadEntity(DemoIds.uuid("load-004"), DemoIds.uuid("farm-003"), DemoIds.uuid("hplan-006"), "MTZ-7D88", "SORRISO_TERMINAL", BigDecimal("40"), LoadStatus.QUEUED.name, null),
            LogisticsLoadEntity(DemoIds.uuid("load-005"), DemoIds.uuid("farm-004"), DemoIds.uuid("hplan-008"), "MTZ-4E21", "PRIMAVERA_SILO", BigDecimal("30"), LoadStatus.DISPATCHED.name, now.minus(2, ChronoUnit.HOURS)),
            LogisticsLoadEntity(DemoIds.uuid("load-006"), DemoIds.uuid("farm-005"), DemoIds.uuid("hplan-009"), "MSX-8F55", "DOURADOS_WAREHOUSE", BigDecimal("26"), LoadStatus.QUEUED.name, null),
            LogisticsLoadEntity(DemoIds.uuid("load-007"), DemoIds.uuid("farm-006"), DemoIds.uuid("hplan-010"), "MTZ-3G77", "PARECIS_TERMINAL", BigDecimal("38"), LoadStatus.QUEUED.name, null),
            LogisticsLoadEntity(DemoIds.uuid("load-008"), DemoIds.uuid("farm-007"), DemoIds.uuid("hplan-011"), "MTZ-5H12", "PEDRA_PRETA_SILO", BigDecimal("33"), LoadStatus.DELIVERED.name, now.minus(1, ChronoUnit.DAYS)),
            LogisticsLoadEntity(DemoIds.uuid("load-009"), DemoIds.uuid("farm-008"), DemoIds.uuid("hplan-012"), "MSX-6J90", "CHAPADAO_TERMINAL", BigDecimal("29"), LoadStatus.QUEUED.name, null),
            LogisticsLoadEntity(DemoIds.uuid("load-010"), DemoIds.uuid("farm-001"), DemoIds.uuid("hplan-001"), "MSX-7K33", "CENTRAL_WAREHOUSE", BigDecimal("31"), LoadStatus.QUEUED.name, null),
        )
        loads.saveAll(loadRows)

        val unitRows = listOf(
            StorageUnitEntity(DemoIds.uuid("sunit-001"), DemoIds.uuid("farm-001"), "SILO_01", BigDecimal("2000"), BigDecimal("820"), "SILO"),
            StorageUnitEntity(DemoIds.uuid("sunit-002"), DemoIds.uuid("farm-008"), "WAREHOUSE_NE", BigDecimal("1500"), BigDecimal("410"), "WAREHOUSE"),
            StorageUnitEntity(DemoIds.uuid("sunit-003"), DemoIds.uuid("farm-002"), "SILO_NORTH", BigDecimal("3000"), BigDecimal("1200"), "SILO"),
            StorageUnitEntity(DemoIds.uuid("sunit-004"), DemoIds.uuid("farm-003"), "SILO_A", BigDecimal("5000"), BigDecimal("2100"), "SILO"),
            StorageUnitEntity(DemoIds.uuid("sunit-005"), DemoIds.uuid("farm-004"), "PRIMAVERA_SILO", BigDecimal("2800"), BigDecimal("950"), "SILO"),
            StorageUnitEntity(DemoIds.uuid("sunit-006"), DemoIds.uuid("farm-005"), "WAREHOUSE_CA", BigDecimal("1800"), BigDecimal("620"), "WAREHOUSE"),
            StorageUnitEntity(DemoIds.uuid("sunit-007"), DemoIds.uuid("farm-006"), "SILO_VV", BigDecimal("3500"), BigDecimal("1400"), "SILO"),
            StorageUnitEntity(DemoIds.uuid("sunit-008"), DemoIds.uuid("farm-007"), "SILO_ES", BigDecimal("2200"), BigDecimal("780"), "SILO"),
        )
        units.saveAll(unitRows)

        val lotRows = listOf(
            StorageLotEntity(DemoIds.uuid("slot-001"), DemoIds.uuid("sunit-001"), DemoIds.uuid("farm-001"), "SOY", BigDecimal("520"), "STANDARD", now.minus(10, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-002"), DemoIds.uuid("sunit-001"), DemoIds.uuid("farm-001"), "SOY", BigDecimal("300"), "PREMIUM", now.minus(3, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-003"), DemoIds.uuid("sunit-003"), DemoIds.uuid("farm-002"), "CORN", BigDecimal("800"), "STANDARD", now.minus(15, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-004"), DemoIds.uuid("sunit-004"), DemoIds.uuid("farm-003"), "SOY", BigDecimal("1500"), "STANDARD", now.minus(7, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-005"), DemoIds.uuid("sunit-005"), DemoIds.uuid("farm-004"), "SOY", BigDecimal("640"), "STANDARD", now.minus(4, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-006"), DemoIds.uuid("sunit-006"), DemoIds.uuid("farm-005"), "CORN", BigDecimal("410"), "STANDARD", now.minus(12, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-007"), DemoIds.uuid("sunit-007"), DemoIds.uuid("farm-006"), "SOY", BigDecimal("980"), "PREMIUM", now.minus(6, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-008"), DemoIds.uuid("sunit-008"), DemoIds.uuid("farm-007"), "SOY", BigDecimal("550"), "STANDARD", now.minus(2, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-009"), DemoIds.uuid("sunit-002"), DemoIds.uuid("farm-008"), "CORN", BigDecimal("280"), "STANDARD", now.minus(20, ChronoUnit.DAYS)),
            StorageLotEntity(DemoIds.uuid("slot-010"), DemoIds.uuid("sunit-004"), DemoIds.uuid("farm-003"), "COTTON", BigDecimal("420"), "STANDARD", now.minus(9, ChronoUnit.DAYS)),
        )
        lots.saveAll(lotRows)
    }

    private fun validateCapacity(capacityT: BigDecimal, usedT: BigDecimal) {
        if (capacityT <= BigDecimal.ZERO || usedT < BigDecimal.ZERO || usedT > capacityT) {
            throw DomainException("STORAGE_CAPACITY_INVALID", "usedT must be between 0 and capacityT")
        }
    }

    private fun HarvestPlanEntity.toDto() = HarvestPlanDto(id, farmId, fieldId, crop, plannedStart, plannedEnd, expectedTHa, status)
    private fun HarvestYieldEntity.toDto() = HarvestYieldDto(id, farmId, fieldId, DemoCatalog.fieldName(fieldId), planId, recordedAt, yieldTHa, moisturePct, areaHa)
    private fun LogisticsLoadEntity.toDto() = LogisticsLoadDto(id, farmId, planId, truckPlate, destination, tons, status, dispatchedAt)
    private fun StorageUnitEntity.toDto() = StorageUnitDto(id, farmId, name, capacityT, usedT, type)
    private fun StorageLotEntity.toDto() = StorageLotDto(id, unitId, farmId, crop, tons, quality, receivedAt)
}

@Service
class HarvestSeed(private val svc: HarvestService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedHarvest() = ApplicationRunner { if (seed) svc.seed() }
}
