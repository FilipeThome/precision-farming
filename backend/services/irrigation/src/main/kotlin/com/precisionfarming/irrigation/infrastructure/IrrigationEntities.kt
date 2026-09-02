package com.precisionfarming.irrigation.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "irrigation_assets")
class IrrigationAssetEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID?,
    val name: String,
    val type: String,
    var status: String,
    @Column(name = "capacity_mm_h") val capacityMmH: BigDecimal?,
)

@Entity
@Table(name = "irrigation_recommendations")
class IrrigationRecommendationEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID,
    @Column(name = "asset_id") val assetId: UUID?,
    @Column(name = "recommended_mm") val recommendedMm: BigDecimal,
    @Column(name = "window_start") val windowStart: Instant,
    @Column(name = "window_end") val windowEnd: Instant,
    val reason: String,
    var status: String,
)

@Entity
@Table(name = "irrigation_simulations")
class IrrigationSimulationEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID,
    val mm: BigDecimal,
    @Column(name = "duration_h") val durationH: BigDecimal,
    @Column(name = "estimated_cost") val estimatedCost: BigDecimal,
    @Column(name = "water_m3") val waterM3: BigDecimal,
    @Column(name = "created_at") val createdAt: Instant,
)

interface IrrigationAssetJpaRepository : JpaRepository<IrrigationAssetEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<IrrigationAssetEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<IrrigationAssetEntity>
}

interface IrrigationRecommendationJpaRepository : JpaRepository<IrrigationRecommendationEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<IrrigationRecommendationEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<IrrigationRecommendationEntity>
}

interface IrrigationSimulationJpaRepository : JpaRepository<IrrigationSimulationEntity, UUID>
