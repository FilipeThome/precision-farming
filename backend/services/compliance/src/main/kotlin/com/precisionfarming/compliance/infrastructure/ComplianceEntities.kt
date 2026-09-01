package com.precisionfarming.compliance.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "traceability_records")
class TraceabilityEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID?,
    @Column(name = "lot_code") val lotCode: String,
    val crop: String,
    @Column(name = "event_type") val eventType: String,
    val summary: String,
    @Column(name = "occurred_at") val occurredAt: Instant,
)

@Entity
@Table(name = "esg_metrics")
class EsgMetricEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val metric: String,
    val value: BigDecimal,
    val unit: String,
    @Column(name = "period_label") val periodLabel: String,
    val score: BigDecimal?,
)

interface TraceabilityJpaRepository : JpaRepository<TraceabilityEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<TraceabilityEntity>
    fun findByIdAndFarmId(id: UUID, farmId: UUID): TraceabilityEntity?
}

interface EsgMetricJpaRepository : JpaRepository<EsgMetricEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<EsgMetricEntity>
}
