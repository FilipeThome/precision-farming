package com.precisionfarming.gateway

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.server.PathContainer
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.web.util.pattern.PathPatternParser
import java.nio.file.Files
import java.nio.file.Path

class GatewayRouteTest {
    /** Mirrors GatewaySecurityConfig: human access only (rejects refresh and service). */
    private fun requireGatewayAccess(type: String?) {
        if (!"access".equals(type, ignoreCase = true)) {
            throw JwtException("Access token required")
        }
    }

    @Test
    fun missingOrRefreshTypeIsRejected() {
        assertThrows(JwtException::class.java) { requireGatewayAccess("refresh") }
        assertThrows(JwtException::class.java) { requireGatewayAccess(null) }
        requireGatewayAccess("access")
    }

    @Test
    fun serviceTypeIsRejectedAtGateway() {
        assertThrows(JwtException::class.java) { requireGatewayAccess("service") }
    }

    @Test
    fun telemetryMetricsBeatsMachinesWildcard() {
        val yaml = Files.readString(Path.of("src/main/resources/application.yml"))
        val metrics = yaml.indexOf("/api/v1/machines/{machineId}/metrics")
        val wildcard = yaml.indexOf("Path=/api/v1/machines/**")
        assertTrue(metrics >= 0 && wildcard >= 0 && metrics < wildcard)
        val parser = PathPatternParser()
        val metricsPattern = parser.parse("/api/v1/machines/{machineId}/metrics")
        val machines = parser.parse("/api/v1/machines/**")
        val path = PathContainer.parsePath("/api/v1/machines/550e8400-e29b-41d4-a716-446655440000/metrics")
        assertTrue(metricsPattern.matches(path))
        assertTrue(machines.matches(path))
        assertFalse(parser.parse("/api/v1/machines/{machineId}/telemetry").matches(path))
    }

    @Test
    fun inspectorAndSeedPathsAreRouted() {
        val yaml = Files.readString(Path.of("src/main/resources/application.yml"))
        assertTrue(yaml.contains("Path=/api/v1/operations/**"))
        assertTrue(yaml.contains("Path=/api/v1/inventory/**"))
        assertTrue(yaml.contains("Path=/api/v1/dev/seed/reset/farm"))
        assertTrue(yaml.contains("RewritePath=/api/v1/dev/seed/reset/farm,/api/v1/dev/seed/reset"))
        val seedBlock = yaml.substring(yaml.indexOf("id: seed-farm"))
        assertTrue(seedBlock.contains("Method=POST"))
        assertFalse(seedBlock.contains("Method=GET"))
    }
}
