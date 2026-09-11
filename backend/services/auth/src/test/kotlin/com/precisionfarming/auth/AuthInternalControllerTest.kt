package com.precisionfarming.auth

import com.precisionfarming.auth.api.AuthInternalController
import com.precisionfarming.auth.api.ServiceTokenRequest
import com.precisionfarming.auth.application.AuthService
import com.precisionfarming.common.UnauthorizedException
import com.precisionfarming.security.JwtProperties
import com.precisionfarming.security.issuer.JwtIssuer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthInternalControllerTest {
    private val authService = mockk<AuthService>(relaxUnitFun = true)
    private val jwtIssuer = mockk<JwtIssuer>()
    private val secret = "s".repeat(32)
    private val props = JwtProperties(serviceMintSecret = secret, authInternalSecret = secret)
    private val controller = AuthInternalController(authService, jwtIssuer, props)

    @Test
    fun validSecretFromPublicIpIsDenied() {
        assertThrows(UnauthorizedException::class.java) {
            controller.mintServiceToken(secret, tokenRequest(), request("8.8.8.8"))
        }
        verify(exactly = 0) { jwtIssuer.createServiceToken(any(), any(), any(), any()) }
    }

    @Test
    fun validSecretFromHostnameIsDenied() {
        assertThrows(UnauthorizedException::class.java) {
            controller.mintServiceToken(secret, tokenRequest(), request("localhost"))
        }
        verify(exactly = 0) { jwtIssuer.createServiceToken(any(), any(), any(), any()) }
    }

    @Test
    fun validSecretFromLoopbackIsAllowed() {
        every { jwtIssuer.createServiceToken(any(), any(), any(), any()) } returns "tok"
        val res = controller.mintServiceToken(secret, tokenRequest(), request("127.0.0.1"))
        assertEquals("tok", res.accessToken)
    }

    private fun tokenRequest() = ServiceTokenRequest(UUID.randomUUID(), listOf(UUID.randomUUID()))

    private fun request(ip: String): HttpServletRequest {
        val req = mockk<HttpServletRequest>()
        every { req.remoteAddr } returns ip
        return req
    }
}
