package com.precisionfarming.auth.application

import com.precisionfarming.common.TooManyRequestsException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory sliding-window limiter for login/refresh (credential stuffing mitigation).
 * Keyed by client IP + optional email. Suitable for single-instance demo/MVP.
 */
@Component
class AuthRateLimiter(
    @Value("\${app.security.auth-rate-limit-per-minute:20}") private val limitPerMinute: Int,
) {
    private data class Window(val startedAtMs: Long, val count: AtomicInteger)

    private val windows = ConcurrentHashMap<String, Window>()

    fun check(key: String) {
        val now = System.currentTimeMillis()
        val window = windows.compute(key) { _, existing ->
            if (existing == null || now - existing.startedAtMs >= 60_000L) {
                Window(now, AtomicInteger(0))
            } else {
                existing
            }
        }!!
        val n = window.count.incrementAndGet()
        if (n > limitPerMinute.coerceAtLeast(1)) {
            throw TooManyRequestsException(
                "Too many authentication attempts. Try again in a minute.",
                "AUTH_RATE_LIMITED",
            )
        }
    }
}
