package com.precisionfarming.auth.api

import com.precisionfarming.auth.application.AuthRateLimiter
import com.precisionfarming.auth.application.AuthService
import com.precisionfarming.auth.application.LoginCommand
import com.precisionfarming.auth.application.RefreshCommand
import com.precisionfarming.auth.application.TokenResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class LoginRequest(@field:Email val email: String, @field:NotBlank val password: String)
data class RefreshRequest(@field:NotBlank val refreshToken: String)
data class MeResponse(val id: UUID, val name: String, val email: String, val role: String)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val rateLimiter: AuthRateLimiter,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest, request: HttpServletRequest): TokenResponse {
        val email = body.email.lowercase()
        rateLimiter.check("login:${clientKey(request)}:$email")
        return authService.login(LoginCommand(email, body.password))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody body: RefreshRequest, request: HttpServletRequest): TokenResponse {
        rateLimiter.check("refresh:${clientKey(request)}")
        return authService.refresh(RefreshCommand(body.refreshToken))
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): MeResponse {
        val user = authService.me(UUID.fromString(jwt.subject))
        return MeResponse(user.id, user.name, user.email, user.role.name)
    }

    private fun clientKey(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")?.substringBefore(',')?.trim()
        return forwarded?.takeIf { it.isNotBlank() } ?: (request.remoteAddr ?: "unknown")
    }
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class SeedController(private val authService: AuthService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset(): Map<String, String> {
        authService.seed()
        return mapOf("status" to "seeded", "service" to "auth")
    }
}
