package com.precisionfarming.telemetry

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.telemetry.application.TelemetryMetrics
import com.precisionfarming.telemetry.application.TelemetrySample
import com.precisionfarming.telemetry.application.TelemetryService
import com.precisionfarming.telemetry.infrastructure.TelemetryJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class TelemetryDomainTest {
    private val repo = mockk<TelemetryJpaRepository>()
    private val svc = TelemetryService(repo)
    private val scope = AccessScope(DemoTenant.ID, setOf(DemoIds.uuid("farm-001")), "OPERATOR")

    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("tel-machine-004-2026090112"), DemoIds.uuid("tel-machine-004-2026090112"))
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

    @Test
    fun metricsEmptyReturnsZeros() {
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val to = from.plus(2, ChronoUnit.DAYS)
        every {
            repo.findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(DemoIds.uuid("machine-001"), from, to)
        } returns emptyList()

        val dto = svc.metrics(scope, DemoIds.uuid("machine-001"), from, to)
        assertEquals(0.0, dto.engineHours)
        assertNull(dto.lastObservedAt)
        assertTrue(dto.days.isEmpty())
    }

    @Test
    fun aggregateSumsHoursAndBucketsUtcDays() {
        val dto = TelemetryMetrics.aggregate(
            listOf(
                TelemetrySample(Instant.parse("2026-09-01T10:00:00Z"), 10.0, 80.0),
                TelemetrySample(Instant.parse("2026-09-01T11:00:00Z"), 8.0, 78.0),
                TelemetrySample(Instant.parse("2026-09-01T12:00:00Z"), 0.0, 76.0),
            ),
        )
        assertEquals(2.0, dto.engineHours)
        assertEquals("2026-09-01", dto.days.single().day)
        assertEquals(2.0, dto.days.single().hours)
    }

    @Test
    fun aggregateAddsLastMovingInterval() {
        val dto = TelemetryMetrics.aggregate(
            listOf(
                TelemetrySample(Instant.parse("2026-09-01T10:00:00Z"), 10.0, 80.0),
                TelemetrySample(Instant.parse("2026-09-01T11:00:00Z"), 8.0, 78.0),
            ),
        )
        assertEquals(2.0, dto.engineHours)
        assertEquals(2.0, dto.days.single().hours)
    }

    @Test
    fun metricsUnknownMachineIsNotFound() {
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val to = from.plus(2, ChronoUnit.DAYS)
        val ex = assertThrows(com.precisionfarming.common.NotFoundException::class.java) {
            svc.metrics(scope, UUID.randomUUID(), from, to)
        }
        assertEquals("MACHINE_NOT_FOUND", ex.code)
    }
}
