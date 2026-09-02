package com.precisionfarming.auth.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class UserFarmId(
    @Column(name = "user_id") val userId: UUID = UUID(0, 0),
    @Column(name = "farm_id") val farmId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "user_farms")
class UserFarmEntity(
    @EmbeddedId val id: UserFarmId,
)

interface UserFarmJpaRepository : JpaRepository<UserFarmEntity, UserFarmId> {
    fun findByIdUserId(userId: UUID): List<UserFarmEntity>
    fun deleteByIdUserIdAndIdFarmId(userId: UUID, farmId: UUID)
    fun deleteByIdFarmId(farmId: UUID)
}

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id val id: UUID,
    @Column(name = "user_id", nullable = false) val userId: UUID,
    @Column(nullable = false, unique = true) val jti: String,
    @Column(name = "expires_at", nullable = false) val expiresAt: Instant,
    @Column(name = "revoked_at") var revokedAt: Instant? = null,
)

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshTokenEntity t where t.jti = :jti")
    fun findByJtiForUpdate(jti: String): RefreshTokenEntity?

    fun findByUserId(userId: UUID): List<RefreshTokenEntity>
}
