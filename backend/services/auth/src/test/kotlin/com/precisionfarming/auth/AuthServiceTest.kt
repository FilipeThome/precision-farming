package com.precisionfarming.auth

import com.precisionfarming.auth.application.AuthService
import com.precisionfarming.auth.application.RefreshCommand
import com.precisionfarming.auth.domain.UserRole
import com.precisionfarming.auth.domain.UserStatus
import com.precisionfarming.auth.infrastructure.UserEntity
import com.precisionfarming.auth.infrastructure.UserJpaRepository
import com.precisionfarming.common.UnauthorizedException
import com.precisionfarming.security.JwtProperties
import com.precisionfarming.security.JwtService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {
    private val users = mockk<UserJpaRepository>()
    private val encoder = BCryptPasswordEncoder()
    private val jwt = JwtService(JwtProperties())
    private val svc = AuthService(users, encoder, jwt)

    @Test
    fun refreshRejectsDisabledUser() {
        val user = user(UserStatus.DISABLED)
        every { users.findById(user.id) } returns Optional.of(user)

        assertThrows(UnauthorizedException::class.java) {
            svc.refresh(RefreshCommand(jwt.createRefreshToken(user.id)))
        }
    }

    @Test
    fun refreshIssuesTokensForActiveUser() {
        val user = user(UserStatus.ACTIVE)
        every { users.findById(user.id) } returns Optional.of(user)

        val tokens = svc.refresh(RefreshCommand(jwt.createRefreshToken(user.id)))

        assertEquals(user.id, tokens.userId)
        assertEquals("ADMIN", tokens.role)
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
