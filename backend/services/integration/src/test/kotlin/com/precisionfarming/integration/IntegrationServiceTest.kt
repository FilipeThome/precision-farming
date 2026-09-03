package com.precisionfarming.integration

import com.precisionfarming.common.DemoIds
import com.precisionfarming.integration.application.IntegrationService
import com.precisionfarming.integration.infrastructure.ConnectorEntity
import com.precisionfarming.integration.infrastructure.ConnectorJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntegrationServiceTest {
    private val repo = mockk<ConnectorJpaRepository>()
    private val svc = IntegrationService(repo)

    @Test
    fun listSplitsCapabilitiesFromDatabase() {
        every { repo.findAll() } returns listOf(
            ConnectorEntity(
                DemoIds.uuid("conn-001"), "DemoJohnDeere", "MACHINE", "DEMO",
                "listMachines,getTelemetry",
            ),
        )
        val rows = svc.list()
        assertEquals(1, rows.size)
        assertEquals("DemoJohnDeere", rows[0].name)
        assertEquals(listOf("listMachines", "getTelemetry"), rows[0].capabilities)
    }
}
