package com.precisionfarming.harvest

import com.precisionfarming.common.DomainException
import com.precisionfarming.harvest.application.CreateHarvestPlan
import com.precisionfarming.harvest.application.HarvestService
import com.precisionfarming.harvest.application.UpsertStorageUnit
import com.precisionfarming.harvest.infrastructure.HarvestPlanJpaRepository
import com.precisionfarming.harvest.infrastructure.HarvestPlanEntity
import com.precisionfarming.harvest.infrastructure.HarvestYieldJpaRepository
import com.precisionfarming.harvest.infrastructure.LogisticsLoadJpaRepository
import com.precisionfarming.harvest.infrastructure.StorageLotEntity
import com.precisionfarming.harvest.infrastructure.StorageLotJpaRepository
import com.precisionfarming.harvest.infrastructure.StorageUnitEntity
import com.precisionfarming.harvest.infrastructure.StorageUnitJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

class HarvestServiceTest {
    private val plans = mockk<HarvestPlanJpaRepository>()
    private val yields = mockk<HarvestYieldJpaRepository>()
    private val loads = mockk<LogisticsLoadJpaRepository>()
    private val units = mockk<StorageUnitJpaRepository>()
    private val lots = mockk<StorageLotJpaRepository>()
    private val fieldFarms = mockk<com.precisionfarming.security.FieldFarmGuard>(relaxUnitFun = true)
    private val svc = HarvestService(plans, yields, loads, units, lots, fieldFarms)
    private val farmId = UUID.randomUUID()
    private val scope = AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER")

    @Test
    fun createUnitIgnoresClientUsedT() {
        every { units.save(any<StorageUnitEntity>()) } answers { firstArg() }
        val created = svc.createUnit(
            scope,
            UpsertStorageUnit(farmId, "SILO_NEW", "SILO", BigDecimal("2000"), BigDecimal("50")),
        )
        assertEquals(BigDecimal.ZERO, created.usedT)
        assertEquals(BigDecimal("2000"), created.capacityT)
    }

    @Test
    fun patchUnitComputesUsedTFromLots() {
        every { units.save(any<StorageUnitEntity>()) } answers { firstArg() }
        val created = svc.createUnit(
            scope,
            UpsertStorageUnit(farmId, "SILO_NEW", "SILO", BigDecimal("2000"), BigDecimal.ZERO),
        )
        every { units.findById(created.id) } returns Optional.of(
            StorageUnitEntity(created.id, farmId, created.name, created.capacityT, created.usedT, created.type),
        )
        every { lots.findByUnitId(created.id) } returns listOf(
            StorageLotEntity(UUID.randomUUID(), created.id, farmId, "SOY", BigDecimal("7"), "STANDARD", Instant.EPOCH),
            StorageLotEntity(UUID.randomUUID(), created.id, farmId, "SOY", BigDecimal("3"), "STANDARD", Instant.EPOCH),
        )
        val patched = svc.patchUnit(
            scope,
            created.id,
            UpsertStorageUnit(farmId, "SILO_NEW", "SILO", BigDecimal("2500"), BigDecimal("999")),
        )
        assertEquals(BigDecimal("2500"), patched.capacityT)
        assertEquals(BigDecimal("10"), patched.usedT)
    }

    @Test
    fun patchUnitMovesLotsAndRecomputesUsedT() {
        val farmB = UUID.randomUUID()
        val both = AccessScope(DemoTenant.ID, setOf(farmId, farmB), "FARM_MANAGER")
        val unitId = UUID.randomUUID()
        val lot = StorageLotEntity(
            UUID.randomUUID(), unitId, farmId, "SOY", BigDecimal("12"), "STANDARD", Instant.EPOCH,
        )
        every { units.findById(unitId) } returns Optional.of(
            StorageUnitEntity(unitId, farmId, "SILO", BigDecimal("2000"), BigDecimal("12"), "SILO"),
        )
        every { units.save(any<StorageUnitEntity>()) } answers { firstArg() }
        every { lots.findByUnitId(unitId) } returns listOf(lot)
        every { lots.saveAll(any<Iterable<StorageLotEntity>>()) } answers { firstArg<Iterable<StorageLotEntity>>().toList() }
        val patched = svc.patchUnit(
            both,
            unitId,
            UpsertStorageUnit(farmB, "SILO", "SILO", BigDecimal("2000"), BigDecimal("999")),
        )
        assertEquals(farmB, patched.farmId)
        assertEquals(farmB, lot.farmId)
        assertEquals(BigDecimal("12"), patched.usedT)
    }

    @Test
    fun createUnitRejectsNonPositiveCapacity() {
        val ex = assertThrows(DomainException::class.java) {
            svc.createUnit(scope, UpsertStorageUnit(farmId, "SILO", "SILO", BigDecimal.ZERO, BigDecimal("20")))
        }
        assertEquals("STORAGE_CAPACITY_INVALID", ex.code)
    }

    @Test
    fun createPlanAcceptsFieldConfirmedByGuard() {
        every { plans.save(any<HarvestPlanEntity>()) } answers { firstArg() }
        val fieldId = UUID.randomUUID()
        val created = svc.createPlan(
            scope,
            CreateHarvestPlan(farmId, fieldId, "SOY", BigDecimal("3.2")),
        )
        assertEquals(fieldId, created.fieldId)
        assertEquals(farmId, created.farmId)
    }

    @Test
    fun createPlanHonorsDates() {
        every { plans.save(any<HarvestPlanEntity>()) } answers { firstArg() }
        val fieldId = UUID.randomUUID()
        val start = Instant.parse("2026-10-01T00:00:00Z")
        val end = start.plus(10, ChronoUnit.DAYS)
        val created = svc.createPlan(
            scope,
            CreateHarvestPlan(farmId, fieldId, "SOY", BigDecimal("3.2"), start, end),
        )
        assertEquals(start, created.plannedStart)
        assertEquals(end, created.plannedEnd)
    }

    @Test
    fun createPlanRejectsWhenFieldGuardDenies() {
        val fieldId = UUID.randomUUID()
        every { fieldFarms.requireBelongsToFarm(fieldId, farmId) } throws
            com.precisionfarming.common.NotFoundException("FIELD_NOT_FOUND", "Field not found")
        val ex = assertThrows(com.precisionfarming.common.NotFoundException::class.java) {
            svc.createPlan(scope, CreateHarvestPlan(farmId, fieldId, "SOY", BigDecimal("3.2")))
        }
        assertEquals("FIELD_NOT_FOUND", ex.code)
    }
}
