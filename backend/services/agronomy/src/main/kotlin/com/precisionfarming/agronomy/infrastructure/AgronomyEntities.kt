package com.precisionfarming.agronomy.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "scouting_observations")
class ScoutingEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID,
    @Column(name = "observed_at") val observedAt: Instant,
    val pest: String?,
    val severity: String,
    val notes: String?,
    var status: String,
)

@Entity
@Table(name = "soil_samples")
class SoilSampleEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID,
    @Column(name = "sampled_at") val sampledAt: Instant,
    var ph: BigDecimal?,
    @Column(name = "organic_matter_pct") var organicMatterPct: BigDecimal?,
    @Column(name = "p_ppm") var pPpm: BigDecimal?,
    @Column(name = "k_ppm") var kPpm: BigDecimal?,
    @Column(name = "lab_ref") var labRef: String?,
    var status: String,
)

@Entity
@Table(name = "recommendations")
class RecommendationEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID?,
    val kind: String,
    val title: String,
    val summary: String,
    val priority: String,
    var status: String,
    @Column(name = "created_at") val createdAt: Instant,
)

@Entity
@Table(name = "prescriptions")
class PrescriptionEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID,
    val product: String,
    val rate: BigDecimal,
    val unit: String,
    var status: String,
    @Column(name = "created_at") val createdAt: Instant,
    @Column(name = "approved_at") var approvedAt: Instant? = null,
)

interface ScoutingJpaRepository : JpaRepository<ScoutingEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<ScoutingEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<ScoutingEntity>
}

interface SoilSampleJpaRepository : JpaRepository<SoilSampleEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<SoilSampleEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<SoilSampleEntity>
}

interface RecommendationJpaRepository : JpaRepository<RecommendationEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<RecommendationEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<RecommendationEntity>
}

interface PrescriptionJpaRepository : JpaRepository<PrescriptionEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<PrescriptionEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<PrescriptionEntity>
}
