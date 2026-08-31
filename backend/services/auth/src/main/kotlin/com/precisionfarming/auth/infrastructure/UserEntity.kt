package com.precisionfarming.auth.infrastructure

import com.precisionfarming.auth.domain.UserRole
import com.precisionfarming.auth.domain.UserStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id val id: UUID,
    @Column(nullable = false) var name: String,
    @Column(nullable = false, unique = true) var email: String,
    @Column(name = "password_hash", nullable = false) var passwordHash: String,
    @Enumerated(EnumType.STRING) var role: UserRole,
    @Enumerated(EnumType.STRING) var status: UserStatus,
)

interface UserJpaRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findByEmailIn(emails: Collection<String>): List<UserEntity>
}
