package com.precisionfarming.auth

import com.precisionfarming.auth.application.AuthService
import com.precisionfarming.auth.application.RefreshCommand
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
import com.precisionfarming.security.issuer.JwtIssuer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AuthServiceTest {
    private val users = mockk<UserJpaRepository>()
    private val memberships = mockk<UserFarmJpaRepository>(relaxed = true)
    private val refreshTokens = mockk<RefreshTokenJpaRepository>(relaxed = true)
    private val encoder = BCryptPasswordEncoder()
    private val jwt = JwtIssuer(RsaTestKeys.props())
    private val svc = AuthService(users, memberships, refreshTokens, encoder, jwt)

    @Test
    fun refreshRejectsDisabledUser() {
        val user = user(UserStatus.DISABLED)
        every { users.findById(user.id) } returns Optional.of(user)
        val issued = jwt.createRefreshToken(user.id)

        assertThrows(UnauthorizedException::class.java) {
            svc.refresh(RefreshCommand(issued.token))
        }
    }

    @Test
    fun refreshIssuesTokensForActiveUser() {
        val user = user(UserStatus.ACTIVE)
        val farm = DemoIds.uuid("farm-001")
        every { users.findById(user.id) } returns Optional.of(user)
        every { memberships.findByIdUserId(user.id) } returns listOf(UserFarmEntity(UserFarmId(user.id, farm)))
        val issued = jwt.createRefreshToken(user.id)
        every { refreshTokens.findByJtiForUpdate(issued.jti.toString()) } returns RefreshTokenEntity(
            UUID.randomUUID(), user.id, issued.jti.toString(), issued.expiresAt, null,
        )

        val tokens = svc.refresh(RefreshCommand(issued.token))

        assertEquals(user.id, tokens.userId)
        assertEquals("ADMIN", tokens.role)
        verify { refreshTokens.save(match { it.revokedAt != null }) }
    }

    @Test
    fun reusedRefreshRevokesFamily() {
        val user = user(UserStatus.ACTIVE)
        every { users.findById(user.id) } returns Optional.of(user)
        val issued = jwt.createRefreshToken(user.id)
        every { refreshTokens.findByJtiForUpdate(issued.jti.toString()) } returns RefreshTokenEntity(
            UUID.randomUUID(), user.id, issued.jti.toString(), issued.expiresAt, Instant.now(),
        )
        every { refreshTokens.findByUserId(user.id) } returns emptyList()

        assertThrows(UnauthorizedException::class.java) {
            svc.refresh(RefreshCommand(issued.token))
        }
        verify { refreshTokens.findByUserId(user.id) }
    }

    private fun user(status: UserStatus) = UserEntity(
        id = UUID.randomUUID(),
        name = "Admin",
        email = "admin@precisionfarming.demo",
        passwordHash = encoder.encode("Precision@123"),
        role = UserRole.ADMIN,
        status = status,
    )
}
