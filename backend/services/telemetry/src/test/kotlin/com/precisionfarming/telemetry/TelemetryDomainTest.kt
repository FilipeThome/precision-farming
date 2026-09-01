package com.precisionfarming.telemetry

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.telemetry.application.TelemetryService
import com.precisionfarming.telemetry.infrastructure.TelemetryJpaRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class TelemetryDomainTest {
    private val repo = mockk<TelemetryJpaRepository>()
    private val svc = TelemetryService(repo)
    private val scope = AccessScope(DemoTenant.ID, setOf(DemoIds.uuid("farm-001")), "OPERATOR")

    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("telemetry-001"), DemoIds.uuid("telemetry-001"))
    }

    @Test
    fun historyRejectsRangeOver31Days() {
        val from = Instant.parse("2026-01-01T00:00:00Z")
        val to = from.plus(32, ChronoUnit.DAYS)
        val ex = assertThrows(DomainException::class.java) {
            svc.history(scope, DemoIds.uuid("machine-001"), from, to)
        }
        assertEquals("TELEMETRY_RANGE_EXCEEDED", ex.code)
    }
}
