package com.precisionfarming.harvest

import com.precisionfarming.common.DomainException
import com.precisionfarming.harvest.application.CreateHarvestPlan
import com.precisionfarming.harvest.application.HarvestService
import com.precisionfarming.harvest.application.UpsertStorageUnit
import com.precisionfarming.harvest.infrastructure.HarvestPlanJpaRepository
import com.precisionfarming.harvest.infrastructure.HarvestPlanEntity
import com.precisionfarming.harvest.infrastructure.HarvestYieldJpaRepository
import com.precisionfarming.harvest.infrastructure.LogisticsLoadJpaRepository
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
    fun createAndPatchStorageUnit() {
        every { units.save(any<StorageUnitEntity>()) } answers { firstArg() }
        val created = svc.createUnit(
            scope,
            UpsertStorageUnit(farmId, "SILO_NEW", "SILO", BigDecimal("2000"), BigDecimal.ZERO),
        )
        assertEquals("SILO_NEW", created.name)
        every { units.findById(created.id) } returns Optional.of(
            StorageUnitEntity(created.id, farmId, created.name, created.capacityT, created.usedT, created.type),
        )
        val patched = svc.patchUnit(
            scope,
            created.id,
            UpsertStorageUnit(farmId, "SILO_NEW", "SILO", BigDecimal("2500"), BigDecimal("10")),
        )
        assertEquals(BigDecimal("2500"), patched.capacityT)
        assertEquals(BigDecimal("10"), patched.usedT)
    }

    @Test
    fun rejectsUsedAboveCapacity() {
        val ex = assertThrows(DomainException::class.java) {
            svc.createUnit(scope, UpsertStorageUnit(farmId, "SILO", "SILO", BigDecimal("10"), BigDecimal("20")))
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
