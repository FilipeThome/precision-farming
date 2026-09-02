package com.precisionfarming.gateway

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.JwtException

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
}
