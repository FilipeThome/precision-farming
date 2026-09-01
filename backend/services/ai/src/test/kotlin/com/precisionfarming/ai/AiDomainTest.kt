package com.precisionfarming.ai

import com.precisionfarming.ai.application.AiService
import com.precisionfarming.ai.infrastructure.PredictionEntity
import com.precisionfarming.ai.infrastructure.PredictionJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AiDomainTest {
    private val repo = mockk<PredictionJpaRepository>()
    private val svc = AiService(repo)
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
    fun predictionsRejectsFieldOutsideScope() {
        val scope = AccessScope(DemoTenant.ID, setOf(farm2), "OPERATOR")
        assertThrows(ForbiddenException::class.java) {
            svc.predictions(scope, DemoIds.uuid("field-001"))
        }
    }

    private fun prediction(farmId: UUID, entityId: UUID) = PredictionEntity(
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
