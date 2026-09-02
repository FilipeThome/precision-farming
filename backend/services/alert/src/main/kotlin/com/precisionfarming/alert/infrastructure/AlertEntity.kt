package com.precisionfarming.alert.infrastructure

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "alerts")
class AlertEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val severity: String,
    val type: String,
    val title: String,
    val message: String,
    @Column(name = "entity_type") val entityType: String?,
    @Column(name = "entity_id") val entityId: UUID?,
    var status: String,
    @Column(name = "created_at") val createdAt: Instant,
)

interface AlertJpaRepository : JpaRepository<AlertEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<AlertEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<AlertEntity>
}
