package com.precisionfarming.auth

import com.precisionfarming.security.JwtService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class AuthServiceLogicTest {
    @Test
    fun bcryptMatchesDemoPassword() {
        val encoder = BCryptPasswordEncoder()
        val hash = encoder.encode("Precision@123")
        assertTrue(encoder.matches("Precision@123", hash))
    }

    @Test
    fun jwtServiceIssuesSignedToken() {
        val jwt = JwtService(com.precisionfarming.security.JwtProperties())
        val token = jwt.createAccessToken(java.util.UUID.randomUUID(), "a@b.c", "ADMIN")
        assertTrue(token.split(".").size == 3)
        val claims = jwt.parse(token)
        assertTrue(claims["role"] == "ADMIN")
        @Suppress("UNCHECKED_CAST")
        val farmIds = claims["farmIds"] as List<String>
        assertTrue(farmIds.isNotEmpty())
        assertTrue(claims["tenantId"] is String)
        assertTrue(claims["type"] == "access")
    }

    @Test
    fun serviceJwtIsCachedUntilExpiryWindow() {
        val jwt = JwtService(com.precisionfarming.security.JwtProperties())
        val subject = java.util.UUID.randomUUID()
        val farm = com.precisionfarming.common.DemoIds.uuid("farm-001")
        val first = jwt.cachedServiceToken(subject, listOf(farm))
        val second = jwt.cachedServiceToken(subject, listOf(farm))
        assertTrue(first === second)
        val claims = jwt.parse(first)
        assertTrue(claims["type"] == "service")
        assertTrue(claims["role"] == "SERVICE")
    }
}
