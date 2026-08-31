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
        assertTrue(jwt.parse(token)["role"] == "ADMIN")
    }

    @Test
    fun serviceJwtIsCachedUntilExpiryWindow() {
        val jwt = JwtService(com.precisionfarming.security.JwtProperties())
        val userId = java.util.UUID.randomUUID()
        val first = jwt.cachedAccessToken(userId, "svc@internal", "ADMIN")
        val second = jwt.cachedAccessToken(userId, "svc@internal", "ADMIN")
        assertTrue(first === second)
    }
}
