package com.precisionfarming.ai.infrastructure

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "predictions")
class PredictionEntity(
    @Id val id: UUID,
    val type: String,
    @Column(name = "entity_type") val entityType: String,
    @Column(name = "entity_id") val entityId: UUID,
    val score: BigDecimal,
    val confidence: BigDecimal,
    val model: String,
    @Column(name = "model_version") val modelVersion: String,
    @Column(name = "generated_at") val generatedAt: Instant,
    val explanation: String,
    @Column(name = "horizon_hours") val horizonHours: Int?,
)

interface PredictionJpaRepository : JpaRepository<PredictionEntity, UUID> {
    fun findByEntityId(entityId: UUID): List<PredictionEntity>
}
