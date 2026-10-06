package com.precisionfarming.integration.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.integration.infrastructure.ConnectorEntity
import com.precisionfarming.integration.infrastructure.ConnectorJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ConnectorDto(
    val name: String,
    val type: String,
    val mode: String,
    val capabilities: List<String>,
)

@Service
class IntegrationService(private val repo: ConnectorJpaRepository) {
    fun list(): List<ConnectorDto> = repo.findAll().map { it.toDto() }

    @Transactional
    fun seed() {
        val rows = listOf(
            ConnectorEntity(
                DemoIds.uuid("conn-001"), "DemoJohnDeere", "MACHINE", "DEMO",
                "listMachines,getTelemetry",
            ),
            ConnectorEntity(
                DemoIds.uuid("conn-002"), "DemoWeather", "WEATHER", "DEMO",
                "forecast",
            ),
            ConnectorEntity(
                DemoIds.uuid("conn-003"), "DemoSatellite", "SATELLITE", "DEMO",
                "ndviOverlay",
            ),
            ConnectorEntity(
                DemoIds.uuid("conn-004"), "DemoDrone", "DRONE", "DEMO",
                "missions",
            ),
        )
        repo.saveAll(rows)
    }

    private fun ConnectorEntity.toDto() = ConnectorDto(
        name, type, mode, capabilities.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    )
}

@Service
class IntegrationSeed(
    private val svc: IntegrationService,
    private val gate: com.precisionfarming.security.DemoSeedGate,
) {
    @Bean
    fun seedIntegration() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
