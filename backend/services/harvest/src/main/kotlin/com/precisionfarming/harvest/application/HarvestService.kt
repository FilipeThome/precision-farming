package com.precisionfarming.harvest.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
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
    val id: UUID, val farmId: UUID, val fieldId: UUID, val planId: UUID?, val recordedAt: Instant,
    val yieldTHa: BigDecimal, val moisturePct: BigDecimal?, val areaHa: BigDecimal?,
)
data class LogisticsLoadDto(
    val id: UUID, val farmId: UUID, val planId: UUID?, val truckPlate: String, val destination: String,
    val tons: BigDecimal, val status: String, val dispatchedAt: Instant?,
)
data class DispatchRequest(val loadId: UUID)
data class StorageUnitDto(val id: UUID, val farmId: UUID, val name: String, val capacityT: BigDecimal, val usedT: BigDecimal, val type: String)
data class StorageLotDto(val id: UUID, val unitId: UUID, val farmId: UUID, val crop: String, val tons: BigDecimal, val quality: String, val receivedAt: Instant)

@Service
class HarvestService(
    private val plans: HarvestPlanJpaRepository,
    private val yields: HarvestYieldJpaRepository,
    private val loads: LogisticsLoadJpaRepository,
    private val units: StorageUnitJpaRepository,
    private val lots: StorageLotJpaRepository,
) {
    fun listPlans(farmId: UUID?) = (farmId?.let { plans.findByFarmId(it) } ?: plans.findAll()).map { it.toDto() }

    @Transactional
    fun createPlan(cmd: CreateHarvestPlan): HarvestPlanDto {
        val now = Instant.now()
        return plans.save(
            HarvestPlanEntity(
                UUID.randomUUID(), cmd.farmId, cmd.fieldId, cmd.crop,
                now.plus(1, ChronoUnit.DAYS), now.plus(5, ChronoUnit.DAYS), cmd.expectedTHa, HarvestPlanStatus.PLANNED.name,
            ),
        ).toDto()
    }

    fun listYield(farmId: UUID?) = (farmId?.let { yields.findByFarmId(it) } ?: yields.findAll()).map { it.toDto() }
    fun listLoads(farmId: UUID?) = (farmId?.let { loads.findByFarmId(it) } ?: loads.findAll()).map { it.toDto() }

    @Transactional
    fun dispatch(cmd: DispatchRequest): LogisticsLoadDto {
        val e = loads.findById(cmd.loadId).orElseThrow { NotFoundException("LOAD_NOT_FOUND", "Load not found") }
        e.status = LoadStatus.DISPATCHED.name
        e.dispatchedAt = Instant.now()
        return loads.save(e).toDto()
    }

    fun listUnits(farmId: UUID?) = (farmId?.let { units.findByFarmId(it) } ?: units.findAll()).map { it.toDto() }
    fun listLots(farmId: UUID?) = (farmId?.let { lots.findByFarmId(it) } ?: lots.findAll()).map { it.toDto() }

    @Transactional
    fun seed() {
        if (plans.existsById(DemoIds.uuid("hplan-001"))) return
        val now = Instant.now()
        plans.saveAll(
            listOf(
                HarvestPlanEntity(DemoIds.uuid("hplan-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "Soja", now.plus(2, ChronoUnit.DAYS), now.plus(6, ChronoUnit.DAYS), BigDecimal("3.8"), HarvestPlanStatus.PLANNED.name),
                HarvestPlanEntity(DemoIds.uuid("hplan-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "Milho", now.plus(10, ChronoUnit.DAYS), now.plus(16, ChronoUnit.DAYS), BigDecimal("8.2"), HarvestPlanStatus.PLANNED.name),
                HarvestPlanEntity(DemoIds.uuid("hplan-003"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-003"), "Soja", now.minus(5, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), BigDecimal("3.5"), HarvestPlanStatus.COMPLETED.name),
                HarvestPlanEntity(DemoIds.uuid("hplan-004"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "Soja", now.plus(3, ChronoUnit.DAYS), now.plus(8, ChronoUnit.DAYS), BigDecimal("3.9"), HarvestPlanStatus.PLANNED.name),
                HarvestPlanEntity(DemoIds.uuid("hplan-005"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-005"), "Milho", now.plus(20, ChronoUnit.DAYS), now.plus(28, ChronoUnit.DAYS), BigDecimal("7.8"), HarvestPlanStatus.PLANNED.name),
                HarvestPlanEntity(DemoIds.uuid("hplan-006"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "Soja", now.plus(4, ChronoUnit.DAYS), now.plus(9, ChronoUnit.DAYS), BigDecimal("4.1"), HarvestPlanStatus.IN_PROGRESS.name),
            ),
        )
        yields.saveAll(
            listOf(
                HarvestYieldEntity(DemoIds.uuid("hyield-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-003"), DemoIds.uuid("hplan-003"), now.minus(1, ChronoUnit.DAYS), BigDecimal("3.62"), BigDecimal("13.5"), BigDecimal("80")),
                HarvestYieldEntity(DemoIds.uuid("hyield-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), null, now.minus(30, ChronoUnit.DAYS), BigDecimal("3.45"), BigDecimal("14.0"), BigDecimal("120")),
                HarvestYieldEntity(DemoIds.uuid("hyield-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), null, now.minus(40, ChronoUnit.DAYS), BigDecimal("3.90"), BigDecimal("12.8"), BigDecimal("210")),
            ),
        )
        loads.saveAll(
            listOf(
                LogisticsLoadEntity(DemoIds.uuid("load-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("hplan-003"), "MSX-1A23", "Armazém Central", BigDecimal("32"), LoadStatus.QUEUED.name, null),
                LogisticsLoadEntity(DemoIds.uuid("load-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("hplan-003"), "MSX-2B44", "Porto Seco", BigDecimal("28"), LoadStatus.DISPATCHED.name, now.minus(4, ChronoUnit.HOURS)),
                LogisticsLoadEntity(DemoIds.uuid("load-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("hplan-004"), "GOY-9C11", "Silo Santa Helena", BigDecimal("35"), LoadStatus.QUEUED.name, null),
                LogisticsLoadEntity(DemoIds.uuid("load-004"), DemoIds.uuid("farm-003"), DemoIds.uuid("hplan-006"), "MTZ-7D88", "Terminal Sorriso", BigDecimal("40"), LoadStatus.QUEUED.name, null),
            ),
        )
        units.saveAll(
            listOf(
                StorageUnitEntity(DemoIds.uuid("sunit-001"), DemoIds.uuid("farm-001"), "Silo 01", BigDecimal("2000"), BigDecimal("820"), "SILO"),
                StorageUnitEntity(DemoIds.uuid("sunit-002"), DemoIds.uuid("farm-001"), "Armazém 01", BigDecimal("1500"), BigDecimal("410"), "WAREHOUSE"),
                StorageUnitEntity(DemoIds.uuid("sunit-003"), DemoIds.uuid("farm-002"), "Silo Norte", BigDecimal("3000"), BigDecimal("1200"), "SILO"),
                StorageUnitEntity(DemoIds.uuid("sunit-004"), DemoIds.uuid("farm-003"), "Silo A", BigDecimal("5000"), BigDecimal("2100"), "SILO"),
            ),
        )
        lots.saveAll(
            listOf(
                StorageLotEntity(DemoIds.uuid("slot-001"), DemoIds.uuid("sunit-001"), DemoIds.uuid("farm-001"), "Soja", BigDecimal("520"), "STANDARD", now.minus(10, ChronoUnit.DAYS)),
                StorageLotEntity(DemoIds.uuid("slot-002"), DemoIds.uuid("sunit-001"), DemoIds.uuid("farm-001"), "Soja", BigDecimal("300"), "PREMIUM", now.minus(3, ChronoUnit.DAYS)),
                StorageLotEntity(DemoIds.uuid("slot-003"), DemoIds.uuid("sunit-003"), DemoIds.uuid("farm-002"), "Milho", BigDecimal("800"), "STANDARD", now.minus(15, ChronoUnit.DAYS)),
                StorageLotEntity(DemoIds.uuid("slot-004"), DemoIds.uuid("sunit-004"), DemoIds.uuid("farm-003"), "Soja", BigDecimal("1500"), "STANDARD", now.minus(7, ChronoUnit.DAYS)),
            ),
        )
    }

    private fun HarvestPlanEntity.toDto() = HarvestPlanDto(id, farmId, fieldId, crop, plannedStart, plannedEnd, expectedTHa, status)
    private fun HarvestYieldEntity.toDto() = HarvestYieldDto(id, farmId, fieldId, planId, recordedAt, yieldTHa, moisturePct, areaHa)
    private fun LogisticsLoadEntity.toDto() = LogisticsLoadDto(id, farmId, planId, truckPlate, destination, tons, status, dispatchedAt)
    private fun StorageUnitEntity.toDto() = StorageUnitDto(id, farmId, name, capacityT, usedT, type)
    private fun StorageLotEntity.toDto() = StorageLotDto(id, unitId, farmId, crop, tons, quality, receivedAt)
}

@Service
class HarvestSeed(private val svc: HarvestService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedHarvest() = ApplicationRunner { if (seed) svc.seed() }
}
