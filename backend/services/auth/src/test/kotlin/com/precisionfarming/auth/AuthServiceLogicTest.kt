package com.precisionfarming.auth

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.issuer.JwtIssuer
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
    fun jwtIssuerSignsAccessAndService() {
        val jwt = JwtIssuer(RsaTestKeys.props())
        val farm = DemoIds.uuid("farm-001")
        val token = jwt.createAccessToken(java.util.UUID.randomUUID(), "a@b.c", "ADMIN", listOf(farm))
        assertTrue(token.split(".").size == 3)
        val claims = jwt.parse(token)
        assertTrue(claims["role"] == "ADMIN")
        assertTrue(claims["type"] == "access")
        val service = jwt.createServiceToken(java.util.UUID.randomUUID(), listOf(farm))
        assertTrue(jwt.parse(service)["type"] == "service")
    }
}
