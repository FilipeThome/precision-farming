package com.precisionfarming.auth.application

import com.precisionfarming.auth.domain.User
import com.precisionfarming.auth.domain.UserRole
import com.precisionfarming.auth.domain.UserStatus
import com.precisionfarming.auth.infrastructure.UserEntity
import com.precisionfarming.auth.infrastructure.UserJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.UnauthorizedException
import com.precisionfarming.security.DemoFarmDirectory
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val role: String,
    val userId: UUID,
    val name: String,
    val email: String,
)

data class LoginCommand(val email: String, val password: String)
data class RefreshCommand(val refreshToken: String)

@Service
class AuthService(
    private val users: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    fun login(cmd: LoginCommand): TokenResponse {
        val user = users.findByEmail(cmd.email.lowercase())
            ?: throw UnauthorizedException("Invalid credentials")
        if (user.status != UserStatus.ACTIVE || !passwordEncoder.matches(cmd.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid credentials")
        }
        return tokens(user)
    }

    fun refresh(cmd: RefreshCommand): TokenResponse {
        val claims = try {
            jwtService.parse(cmd.refreshToken)
        } catch (_: Exception) {
            throw UnauthorizedException("Invalid refresh token")
        }
        if (claims["type"] != "refresh") throw UnauthorizedException("Invalid refresh token")
        val user = users.findById(UUID.fromString(claims.subject)).orElseThrow {
            UnauthorizedException("Invalid refresh token")
        }
        if (user.status != UserStatus.ACTIVE) throw UnauthorizedException("Invalid refresh token")
        return tokens(user)
    }

    fun me(userId: UUID): User {
        val e = users.findById(userId).orElseThrow { UnauthorizedException("Unknown user") }
        return User(e.id, e.name, e.email, e.passwordHash, e.role, e.status)
    }

    private fun tokens(user: UserEntity) = TokenResponse(
        accessToken = jwtService.createAccessToken(
            userId = user.id,
            email = user.email,
            role = user.role.name,
            tenantId = DemoTenant.ID,
            farmIds = DemoFarmDirectory.forRole(user.role.name),
        ),
        refreshToken = jwtService.createRefreshToken(user.id),
        role = user.role.name,
        userId = user.id,
        name = user.name,
        email = user.email,
    )

    @Transactional
    fun seed() {
        val demo = listOf(
            Triple("admin@precisionfarming.demo", "Admin", UserRole.ADMIN),
            Triple("manager@precisionfarming.demo", "Manager", UserRole.FARM_MANAGER),
            Triple("operator@precisionfarming.demo", "Operator", UserRole.OPERATOR),
            Triple("maintenance@precisionfarming.demo", "Maintenance", UserRole.MAINTENANCE),
        )
        val emails = demo.map { it.first }
        val existing = users.findByEmailIn(emails).map { it.email }.toHashSet()
        users.saveAll(
            demo.filter { it.first !in existing }.map { (email, name, role) ->
                UserEntity(
                    id = DemoIds.uuid(email),
                    name = name,
                    email = email,
                    passwordHash = passwordEncoder.encode("Precision@123"),
                    role = role,
                    status = UserStatus.ACTIVE,
                )
            },
        )
    }
}

@Service
class AuthSeed(
    private val authService: AuthService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedUsers() = ApplicationRunner {
        if (seed) authService.seed()
    }
}
