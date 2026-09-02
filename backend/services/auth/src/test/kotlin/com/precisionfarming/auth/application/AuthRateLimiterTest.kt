package com.precisionfarming.auth.application

import com.precisionfarming.common.TooManyRequestsException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AuthRateLimiterTest {
    @Test
    fun allowsUpToLimitThenBlocks() {
        val limiter = AuthRateLimiter(limitPerMinute = 3)
        assertDoesNotThrow { limiter.check("k") }
        assertDoesNotThrow { limiter.check("k") }
        assertDoesNotThrow { limiter.check("k") }
        val ex = assertThrows(TooManyRequestsException::class.java) { limiter.check("k") }
        assertEquals(429, ex.httpStatus)
        assertEquals("AUTH_RATE_LIMITED", ex.code)
    }

    @Test
    fun separateKeysAreIndependent() {
        val limiter = AuthRateLimiter(limitPerMinute = 1)
        assertDoesNotThrow { limiter.check("a") }
        assertDoesNotThrow { limiter.check("b") }
        assertThrows(TooManyRequestsException::class.java) { limiter.check("a") }
    }
}
