package com.precisionfarming.auth

import com.precisionfarming.auth.api.AuthController
import com.precisionfarming.auth.api.RefreshRequest
import com.precisionfarming.auth.application.AuthRateLimiter
import com.precisionfarming.auth.application.AuthService
import com.precisionfarming.auth.application.RefreshCommand
import com.precisionfarming.auth.application.TokenResponse
import com.precisionfarming.common.TooManyRequestsException
import com.precisionfarming.common.UnauthorizedException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthControllerTest {
    private val authService = mockk<AuthService>()
    private val rateLimiter = mockk<AuthRateLimiter>(relaxUnitFun = true)
    private val controller = AuthController(authService, rateLimiter)

    @Test
    fun refreshRateLimitsIpBeforeParsingToken() {
        every { rateLimiter.check("refresh:10.0.0.1") } throws TooManyRequestsException(
            "Too many authentication attempts. Try again in a minute.",
            "AUTH_RATE_LIMITED",
        )

        val ex = assertThrows(TooManyRequestsException::class.java) {
            controller.refresh(RefreshRequest("garbage"), request("10.0.0.1"))
        }

        assertEquals("AUTH_RATE_LIMITED", ex.code)
        verify(exactly = 0) { authService.peekRefreshUserId(any()) }
        verify(exactly = 0) { authService.refresh(any()) }
    }

    @Test
    fun refreshStillRateLimitsIpWhenTokenIsInvalid() {
        every { authService.peekRefreshUserId("garbage") } throws UnauthorizedException("Invalid refresh token")

        assertThrows(UnauthorizedException::class.java) {
            controller.refresh(RefreshRequest("garbage"), request("10.0.0.1"))
        }

        verifyOrder {
            rateLimiter.check("refresh:10.0.0.1")
            authService.peekRefreshUserId("garbage")
        }
        verify(exactly = 0) { rateLimiter.check(match { it.startsWith("refresh-user:") }) }
        verify(exactly = 0) { authService.refresh(any()) }
    }

    @Test
    fun refreshRateLimitsIpThenUserForValidToken() {
        val userId = UUID.randomUUID()
        val tokens = TokenResponse(
            accessToken = "access",
            refreshToken = "refresh",
            role = "ADMIN",
            userId = userId,
            name = "Ana Souza",
            email = "admin@precisionfarming.demo",
        )
        every { authService.peekRefreshUserId("tok") } returns userId
        every { authService.refresh(RefreshCommand("tok")) } returns tokens

        val result = controller.refresh(RefreshRequest("tok"), request("10.0.0.1"))

        assertEquals(tokens, result)
        verifyOrder {
            rateLimiter.check("refresh:10.0.0.1")
            authService.peekRefreshUserId("tok")
            rateLimiter.check("refresh-user:$userId")
            authService.refresh(RefreshCommand("tok"))
        }
    }

    private fun request(ip: String): HttpServletRequest {
        val req = mockk<HttpServletRequest>()
        every { req.remoteAddr } returns ip
        return req
    }
}
