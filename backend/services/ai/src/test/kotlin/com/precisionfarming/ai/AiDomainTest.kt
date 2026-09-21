package com.precisionfarming.ai

import com.precisionfarming.ai.application.AiService
import com.precisionfarming.ai.infrastructure.PredictionEntity
import com.precisionfarming.ai.infrastructure.PredictionJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.security.FieldFarmGuard
import com.precisionfarming.security.MachineFarmGuard
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AiDomainTest {
    private val repo = mockk<PredictionJpaRepository>()
    private val fieldFarms = mockk<FieldFarmGuard>(relaxUnitFun = true)
    private val machineFarms = mockk<MachineFarmGuard>(relaxUnitFun = true)
    private val svc = AiService(repo, fieldFarms, machineFarms)
    private val farm1 = DemoIds.uuid("farm-001")
    private val farm2 = DemoIds.uuid("farm-002")

    @Test
    fun insightsOnlyReturnsRequestedFarm() {
        every { repo.findByFarmIdIn(setOf(farm1)) } returns listOf(prediction(farm1, DemoIds.uuid("field-001")))
        val result = svc.insights(AccessScope(DemoTenant.ID, setOf(farm1, farm2), "OPERATOR"), farm1)
        assertEquals(1, result.size)
        assertEquals(DemoIds.uuid("field-001"), result[0].entityId)
    }

    @Test
    fun predictionsUsesFieldGuard() {
        val fieldId = DemoIds.uuid("field-001")
        val scope = AccessScope(DemoTenant.ID, setOf(farm1), "OPERATOR")
        every { repo.findByEntityId(fieldId) } returns listOf(prediction(farm1, fieldId))
        val result = svc.predictions(scope, fieldId)
        assertEquals(1, result.size)
        verify { fieldFarms.requireRead(scope, fieldId) }
    }

    @Test
    fun machineRiskUsesMachineGuard() {
        val machineId = DemoIds.uuid("machine-001")
        val scope = AccessScope(DemoTenant.ID, setOf(farm1), "OPERATOR")
        every { repo.findByEntityId(machineId) } returns emptyList()
        svc.machineRisk(scope, machineId)
        verify { machineFarms.requireRead(scope, machineId) }
    }

    @Test
    fun predictionsRejectsFieldOutsideScope() {
        val fieldId = DemoIds.uuid("field-001")
        val scope = AccessScope(DemoTenant.ID, setOf(farm2), "OPERATOR")
        every { fieldFarms.requireRead(scope, fieldId) } throws NotFoundException("FIELD_NOT_FOUND", "Not found")
        val ex = assertThrows(NotFoundException::class.java) {
            svc.predictions(scope, fieldId)
        }
        assertEquals("FIELD_NOT_FOUND", ex.code)
    }

    @Test
    fun feedbackUsesStoredFarmIdOnly() {
        val id = UUID.randomUUID()
        val pred = prediction(farm1, DemoIds.uuid("field-001"))
        every { repo.findById(id) } returns Optional.of(pred)
        val result = svc.feedback(AccessScope(DemoTenant.ID, setOf(farm1), "OPERATOR"), id)
        assertEquals(id, result["id"])
        assertEquals("recorded", result["status"])
    }

    @Test
    fun feedbackMissingFarmIdIsNotFound() {
        val id = UUID.randomUUID()
        val pred = prediction(null, DemoIds.uuid("field-001"))
        every { repo.findById(id) } returns Optional.of(pred)
        val ex = assertThrows(NotFoundException::class.java) {
            svc.feedback(AccessScope(DemoTenant.ID, setOf(farm1), "OPERATOR"), id)
        }
        assertEquals("PREDICTION_NOT_FOUND", ex.code)
    }

    private fun prediction(farmId: UUID?, entityId: UUID) = PredictionEntity(
        id = UUID.randomUUID(),
        type = "YIELD_FORECAST",
        entityType = "FIELD",
        entityId = entityId,
        score = BigDecimal("0.5"),
        confidence = BigDecimal("0.5"),
        model = "demo",
        modelVersion = "0.1.0",
        generatedAt = Instant.now(),
        explanation = "a|b",
        horizonHours = 24,
        farmId = farmId,
    )
}
