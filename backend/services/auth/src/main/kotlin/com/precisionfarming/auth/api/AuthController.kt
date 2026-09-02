package com.precisionfarming.auth.api

import com.precisionfarming.auth.application.AuthRateLimiter
import com.precisionfarming.auth.application.AuthService
import com.precisionfarming.auth.application.LoginCommand
import com.precisionfarming.auth.application.RefreshCommand
import com.precisionfarming.auth.application.TokenResponse
import com.precisionfarming.common.PemKeys
import com.precisionfarming.common.UnauthorizedException
import com.precisionfarming.security.JwtProperties
import com.precisionfarming.security.issuer.JwtIssuer
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class LoginRequest(@field:Email val email: String, @field:NotBlank val password: String)
data class RefreshRequest(@field:NotBlank val refreshToken: String)
data class LogoutRequest(val refreshToken: String? = null)
data class MeResponse(val id: UUID, val name: String, val email: String, val role: String)
data class ServiceTokenRequest(val subject: UUID, val farmIds: List<UUID>, val email: String = "service@internal")
data class ServiceTokenResponse(val accessToken: String, val tokenType: String = "Bearer")
data class MembershipRequest(val userId: UUID, val farmId: UUID)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val rateLimiter: AuthRateLimiter,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest, request: HttpServletRequest): TokenResponse {
        val email = body.email.lowercase()
        rateLimiter.check("login:${clientIp(request)}:$email")
        return authService.login(LoginCommand(email, body.password))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody body: RefreshRequest, request: HttpServletRequest): TokenResponse {
        rateLimiter.check("refresh-ip:${clientIp(request)}")
        val tokens = authService.refresh(RefreshCommand(body.refreshToken))
        rateLimiter.check("refresh-user:${tokens.userId}")
        return tokens
    }

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal jwt: Jwt, @RequestBody(required = false) body: LogoutRequest?) {
        authService.logout(UUID.fromString(jwt.subject))
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): MeResponse {
        val user = authService.me(UUID.fromString(jwt.subject))
        return MeResponse(user.id, user.name, user.email, user.role.name)
    }

    private fun clientIp(request: HttpServletRequest): String = request.remoteAddr ?: "unknown"
}

@RestController
class AuthInternalController(
    private val authService: AuthService,
    private val jwtIssuer: JwtIssuer,
    private val props: JwtProperties,
) {
    @PostMapping("/internal/service-tokens")
    fun mintServiceToken(
        @RequestHeader("X-Service-Mint") secret: String?,
        @RequestBody body: ServiceTokenRequest,
    ): ServiceTokenResponse {
        requireMint(secret, props.serviceMintSecret)
        if (body.farmIds.isEmpty() || body.farmIds.size > JwtIssuer.MAX_SERVICE_FARMS) {
            throw UnauthorizedException("Invalid service token request")
        }
        val token = jwtIssuer.createServiceToken(body.subject, body.farmIds.toSet(), email = body.email)
        return ServiceTokenResponse(token)
    }

    @PostMapping("/internal/memberships")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun grant(
        @RequestHeader("X-Auth-Internal") secret: String?,
        @RequestBody body: MembershipRequest,
    ) {
        requireMint(secret, props.authInternalSecret)
        authService.grantFarm(body.userId, body.farmId)
    }

    @PostMapping("/internal/memberships/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @RequestHeader("X-Auth-Internal") secret: String?,
        @RequestParam farmId: UUID,
        @RequestParam(required = false) userId: UUID?,
    ) {
        requireMint(secret, props.authInternalSecret)
        authService.revokeFarm(userId, farmId)
    }

    private fun requireMint(provided: String?, expected: String) {
        if (expected.length < 32 || provided.isNullOrBlank() || !PemKeys.secretsEqual(provided, expected)) {
            throw UnauthorizedException("Invalid credentials")
        }
    }
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class SeedController(private val authService: AuthService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset(): Map<String, String> {
        authService.reset()
        return mapOf("status" to "seeded", "service" to "auth")
    }
}
