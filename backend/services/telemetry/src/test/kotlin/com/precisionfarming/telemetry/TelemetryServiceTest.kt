package com.precisionfarming.telemetry

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.security.MachineFarmGuard
import com.precisionfarming.telemetry.application.TelemetryService
import com.precisionfarming.telemetry.infrastructure.TelemetryJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class TelemetryServiceTest {
    private val repo = mockk<TelemetryJpaRepository>()
    private val machineFarms = mockk<MachineFarmGuard>(relaxUnitFun = true)
    private val svc = TelemetryService(repo, machineFarms)
    private val scope = AccessScope(DemoTenant.ID, setOf(DemoIds.uuid("farm-001")), "OPERATOR")

    @Test
    fun historyRequiresMachineRead() {
        val machineId = UUID.randomUUID()
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val to = from.plus(1, ChronoUnit.DAYS)
        every {
            repo.findByMachineIdAndObservedAtBetweenOrderByObservedAtAsc(machineId, from, to)
        } returns emptyList()
        svc.history(scope, machineId, from, to)
        verify { machineFarms.requireRead(scope, machineId) }
    }

    @Test
    fun historyUnknownMachineIsNotFound() {
        val machineId = UUID.randomUUID()
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val to = from.plus(1, ChronoUnit.DAYS)
        every { machineFarms.requireRead(scope, machineId) } throws
            NotFoundException("MACHINE_NOT_FOUND", "Not found")
        val ex = assertThrows(NotFoundException::class.java) {
            svc.history(scope, machineId, from, to)
        }
        assertEquals("MACHINE_NOT_FOUND", ex.code)
    }
}
