package com.precisionfarming.auth.application

import com.precisionfarming.auth.domain.User
import com.precisionfarming.auth.domain.UserRole
import com.precisionfarming.auth.domain.UserStatus
import com.precisionfarming.auth.infrastructure.RefreshTokenEntity
import com.precisionfarming.auth.infrastructure.RefreshTokenJpaRepository
import com.precisionfarming.auth.infrastructure.UserEntity
import com.precisionfarming.auth.infrastructure.UserFarmEntity
import com.precisionfarming.auth.infrastructure.UserFarmId
import com.precisionfarming.auth.infrastructure.UserFarmJpaRepository
import com.precisionfarming.auth.infrastructure.UserJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.UnauthorizedException
import com.precisionfarming.security.DemoFarmDirectory
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.security.issuer.JwtIssuer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
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
    private val memberships: UserFarmJpaRepository,
    private val refreshTokens: RefreshTokenJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtIssuer: JwtIssuer,
) {
    fun login(cmd: LoginCommand): TokenResponse {
        val user = users.findByEmail(cmd.email.lowercase())
            ?: throw UnauthorizedException("Invalid credentials")
        if (user.status != UserStatus.ACTIVE || !passwordEncoder.matches(cmd.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid credentials")
        }
        return tokens(user)
    }

    fun peekRefreshUserId(refreshToken: String): UUID {
        val claims = parseRefresh(refreshToken)
        return UUID.fromString(claims.subject)
    }

    @Transactional
    fun refresh(cmd: RefreshCommand): TokenResponse {
        val claims = parseRefresh(cmd.refreshToken)
        val jti = claims.id ?: throw UnauthorizedException("Invalid refresh token")
        val user = users.findById(UUID.fromString(claims.subject)).orElseThrow {
            UnauthorizedException("Invalid refresh token")
        }
        if (user.status != UserStatus.ACTIVE) throw UnauthorizedException("Invalid refresh token")
        val row = refreshTokens.findByJtiForUpdate(jti)
        val now = Instant.now()
        if (row == null || row.userId != user.id || row.revokedAt != null || row.expiresAt.isBefore(now)) {
            if (row != null) revokeAll(user.id)
            throw UnauthorizedException("Invalid refresh token")
        }
        row.revokedAt = now
        refreshTokens.save(row)
        return tokens(user)
    }

    @Transactional
    fun logout(userId: UUID) {
        revokeAll(userId)
    }

    fun me(userId: UUID): User {
        val e = users.findById(userId).orElseThrow { UnauthorizedException("Unknown user") }
        return User(e.id, e.name, e.email, e.passwordHash, e.role, e.status)
    }

    @Transactional
    fun grantFarm(userId: UUID, farmId: UUID) {
        val id = UserFarmId(userId, farmId)
        if (!memberships.existsById(id)) {
            memberships.save(UserFarmEntity(id))
        }
    }

    @Transactional
    fun revokeFarm(userId: UUID?, farmId: UUID) {
        if (userId != null) memberships.deleteByIdUserIdAndIdFarmId(userId, farmId)
        else memberships.deleteByIdFarmId(farmId)
    }

    private fun tokens(user: UserEntity): TokenResponse {
        val farmIds = memberships.findByIdUserId(user.id).map { it.id.farmId }
        if (farmIds.isEmpty()) throw UnauthorizedException("Invalid credentials")
        val refresh = jwtIssuer.createRefreshToken(user.id)
        refreshTokens.save(
            RefreshTokenEntity(UUID.randomUUID(), user.id, refresh.jti.toString(), refresh.expiresAt, null),
        )
        return TokenResponse(
            accessToken = jwtIssuer.createAccessToken(
                userId = user.id,
                email = user.email,
                role = user.role.name,
                farmIds = farmIds,
                tenantId = DemoTenant.ID,
            ),
            refreshToken = refresh.token,
            role = user.role.name,
            userId = user.id,
            name = user.name,
            email = user.email,
        )
    }

    private fun revokeAll(userId: UUID) {
        val now = Instant.now()
        refreshTokens.findByUserId(userId).forEach { token ->
            if (token.revokedAt == null) {
                token.revokedAt = now
                refreshTokens.save(token)
            }
        }
    }

    /** Boot: insert missing demo users and memberships. Never rewrite role/status/password. */
    @Transactional
    fun reconcile() {
        seedUsers(resetExisting = false)
    }

    /** Admin reset: canonical names/roles/status/memberships. Does not re-hash existing passwords. */
    @Transactional
    fun reset() {
        seedUsers(resetExisting = true)
    }

    private fun seedUsers(resetExisting: Boolean) {
        val demo = listOf(
            Triple("admin@precisionfarming.demo", "Ana Souza", UserRole.ADMIN),
            Triple("manager@precisionfarming.demo", "Carlos Mendes", UserRole.FARM_MANAGER),
            Triple("operator@precisionfarming.demo", "Juliana Rocha", UserRole.OPERATOR),
            Triple("maintenance@precisionfarming.demo", "Pedro Almeida", UserRole.MAINTENANCE),
        )
        val emails = demo.map { it.first }
        val existing = users.findByEmailIn(emails).associateBy { it.email }
        val saved = users.saveAll(
            demo.map { (email, name, role) ->
                val found = existing[email]
                if (found != null) {
                    found.name = name
                    if (resetExisting) {
                        found.role = role
                        found.status = UserStatus.ACTIVE
                    }
                    found
                } else {
                    UserEntity(
                        id = DemoIds.uuid(email),
                        name = name,
                        email = email,
                        passwordHash = checkNotNull(passwordEncoder.encode("Precision@123")),
                        role = role,
                        status = UserStatus.ACTIVE,
                    )
                }
            },
        )
        saved.forEach { user ->
            val farms = DemoFarmDirectory.forRole(user.role.name)
            if (resetExisting) {
                memberships.findByIdUserId(user.id)
                    .filter { it.id.farmId !in farms }
                    .forEach { memberships.deleteByIdUserIdAndIdFarmId(user.id, it.id.farmId) }
            }
            farms.forEach { farmId ->
                val id = UserFarmId(user.id, farmId)
                if (!memberships.existsById(id)) memberships.save(UserFarmEntity(id))
            }
        }
    }

    private fun parseRefresh(refreshToken: String) = try {
        jwtIssuer.parse(refreshToken)
    } catch (_: Exception) {
        throw UnauthorizedException("Invalid refresh token")
    }.also { claims ->
        if (claims["type"] != "refresh") throw UnauthorizedException("Invalid refresh token")
    }
}

@Service
class AuthSeed(
    private val authService: AuthService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedUsers() = ApplicationRunner {
        if (seed) authService.reconcile()
    }
}
