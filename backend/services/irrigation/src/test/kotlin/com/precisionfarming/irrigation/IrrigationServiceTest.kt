package com.precisionfarming.irrigation

import com.precisionfarming.common.DemoIds
import com.precisionfarming.irrigation.application.IrrigationService
import com.precisionfarming.irrigation.application.UpsertIrrigationAsset
import com.precisionfarming.irrigation.domain.IrrigationSimulator
import com.precisionfarming.irrigation.infrastructure.IrrigationAssetEntity
import com.precisionfarming.irrigation.infrastructure.IrrigationAssetJpaRepository
import com.precisionfarming.irrigation.infrastructure.IrrigationRecommendationJpaRepository
import com.precisionfarming.irrigation.infrastructure.IrrigationSimulationJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class IrrigationServiceTest {
    private val assets = mockk<IrrigationAssetJpaRepository>()
    private val recommendations = mockk<IrrigationRecommendationJpaRepository>()
    private val simulations = mockk<IrrigationSimulationJpaRepository>()
    private val simulator = mockk<IrrigationSimulator>()
    private val fieldFarms = mockk<com.precisionfarming.security.FieldFarmGuard>(relaxUnitFun = true)
    private val svc = IrrigationService(assets, recommendations, simulations, simulator, fieldFarms)
    private val farmId = DemoIds.uuid("farm-001")
    private val scope = AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER")

    @Test
    fun createAndPatchWithAndWithoutField() {
        every { assets.save(any<IrrigationAssetEntity>()) } answers { firstArg() }
        val pump = svc.createAsset(
            scope,
            UpsertIrrigationAsset(farmId, null, "PUMP_01", "PUMP", "IDLE", null),
        )
        assertNull(pump.fieldId)

        val fieldId = DemoIds.uuid("field-001")
        val pivot = svc.createAsset(
            scope,
            UpsertIrrigationAsset(farmId, fieldId, "PIVOT_NEW", "PIVOT", "IDLE", BigDecimal("8.0")),
        )
        assertEquals(fieldId, pivot.fieldId)

        every { assets.findById(pivot.id) } returns Optional.of(
            IrrigationAssetEntity(pivot.id, farmId, fieldId, pivot.name, pivot.type, pivot.status, pivot.capacityMmH),
        )
        val patched = svc.patchAsset(
            scope,
            pivot.id,
            UpsertIrrigationAsset(farmId, fieldId, "PIVOT_NEW", "PIVOT", "RUNNING", BigDecimal("8.5")),
        )
        assertEquals("RUNNING", patched.status)
        assertEquals(BigDecimal("8.5"), patched.capacityMmH)
    }

    @Test
    fun createAssetAcceptsFieldConfirmedByGuard() {
        every { assets.save(any<IrrigationAssetEntity>()) } answers { firstArg() }
        val fieldId = UUID.randomUUID()
        val created = svc.createAsset(
            scope,
            UpsertIrrigationAsset(farmId, fieldId, "PIVOT_NEW", "PIVOT", "IDLE", BigDecimal("8.0")),
        )
        assertEquals(fieldId, created.fieldId)
        assertEquals(farmId, created.farmId)
    }

    @Test
    fun createAssetRejectsWhenFieldGuardDenies() {
        val fieldId = UUID.randomUUID()
        every { fieldFarms.requireBelongsToFarm(fieldId, farmId) } throws
            com.precisionfarming.common.NotFoundException("FIELD_NOT_FOUND", "Field not found")
        val ex = org.junit.jupiter.api.Assertions.assertThrows(
            com.precisionfarming.common.NotFoundException::class.java,
        ) {
            svc.createAsset(
                scope,
                UpsertIrrigationAsset(farmId, fieldId, "PIVOT_NEW", "PIVOT", "IDLE", BigDecimal("8.0")),
            )
        }
        assertEquals("FIELD_NOT_FOUND", ex.code)
    }
}
