package com.precisionfarming.integration.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.access.prepost.PreAuthorize

data class Connector(val name: String, val type: String, val mode: String, val capabilities: List<String>)

@RestController
@RequestMapping("/api/v1/integrations")
class IntegrationController {
    @GetMapping
    fun list() = listOf(
        Connector("DemoJohnDeere", "MACHINE", "DEMO", listOf("listMachines", "getTelemetry")),
        Connector("DemoWeather", "WEATHER", "DEMO", listOf("forecast")),
        Connector("DemoSatellite", "SATELLITE", "DEMO", listOf("ndviOverlay")),
        Connector("DemoDrone", "DRONE", "DEMO", listOf("missions")),
    )
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class IntegrationSeedController {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "integration")
}
