package com.precisionfarming.operation

import com.precisionfarming.common.DemoIds
import com.precisionfarming.operation.application.OperationProgress
import com.precisionfarming.operation.application.WorkSample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class OperationDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("operation-001"), DemoIds.uuid("operation-001"))
    }

    @Test
    fun weightsIncludeTransientStatuses() {
        assertEquals(1.0, OperationProgress.weight("COMPLETED"))
        assertEquals(0.5, OperationProgress.weight("IN_PROGRESS"))
        assertEquals(0.5, OperationProgress.weight("STARTING"))
        assertEquals(0.5, OperationProgress.weight("COMPLETING"))
        assertEquals(0.25, OperationProgress.weight("PAUSED"))
        assertEquals(0.0, OperationProgress.weight("PLANNED"))
    }

    @Test
    fun summarizeWeightsAreaAndInputs() {
        val item = DemoIds.uuid("item-001")
        val dto = OperationProgress.summarize(
            listOf(
                WorkSample(
                    "COMPLETED",
                    BigDecimal("10.0"),
                    item,
                    BigDecimal("20"),
                    Instant.parse("2026-09-01T10:00:00Z"),
                    null,
                ),
                WorkSample(
                    "IN_PROGRESS",
                    BigDecimal("10.0"),
                    item,
                    BigDecimal("20"),
                    Instant.parse("2026-09-01T12:00:00Z"),
                    null,
                ),
                WorkSample("PLANNED", BigDecimal("10.0"), item, BigDecimal("20"), null, Instant.parse("2026-09-02T00:00:00Z")),
            ),
        )
        assertEquals(BigDecimal("15.0"), dto.areaHa)
        assertEquals(1, dto.days.size)
        assertEquals(BigDecimal("15.0"), dto.days.single().areaHa)
        assertEquals(BigDecimal("30.0"), dto.inputs.single().quantity)
    }
}
