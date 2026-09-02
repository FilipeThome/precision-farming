package com.precisionfarming.irrigation.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoFieldFarms
import com.precisionfarming.irrigation.domain.IrrigationSimulator
import com.precisionfarming.irrigation.domain.SimulationInput
import com.precisionfarming.irrigation.infrastructure.IrrigationAssetEntity
import com.precisionfarming.irrigation.infrastructure.IrrigationAssetJpaRepository
import com.precisionfarming.irrigation.infrastructure.IrrigationRecommendationEntity
import com.precisionfarming.irrigation.infrastructure.IrrigationRecommendationJpaRepository
import com.precisionfarming.irrigation.infrastructure.IrrigationSimulationEntity
import com.precisionfarming.irrigation.infrastructure.IrrigationSimulationJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class IrrigationAssetDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID?, val name: String, val type: String,
    val status: String, val capacityMmH: BigDecimal?,
)
data class IrrigationRecommendationDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID, val assetId: UUID?, val recommendedMm: BigDecimal,
    val windowStart: Instant, val windowEnd: Instant, val reason: String, val status: String,
)
data class SimulateRequest(val farmId: UUID, val fieldId: UUID, val mm: BigDecimal, val areaHa: BigDecimal = BigDecimal("100"))
data class SimulationDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID, val mm: BigDecimal, val durationH: BigDecimal,
    val estimatedCost: BigDecimal, val waterM3: BigDecimal, val createdAt: Instant,
)

@Service
class IrrigationService(
    private val assets: IrrigationAssetJpaRepository,
    private val recommendations: IrrigationRecommendationJpaRepository,
    private val simulations: IrrigationSimulationJpaRepository,
    private val simulator: IrrigationSimulator,
) {
    fun listAssets(scope: AccessScope, farmId: UUID?) =
        assets.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }
    fun listRecommendations(scope: AccessScope, farmId: UUID?) =
        recommendations.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun simulate(scope: AccessScope, cmd: SimulateRequest): SimulationDto {
        scope.requireFarm(cmd.farmId)
        DemoFieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        val result = simulator.simulate(SimulationInput(cmd.farmId, cmd.fieldId, cmd.mm, cmd.areaHa))
        return simulations.save(
            IrrigationSimulationEntity(
                UUID.randomUUID(), cmd.farmId, cmd.fieldId, cmd.mm,
                result.durationH, result.estimatedCost, result.waterM3, Instant.now(),
            ),
        ).toDto()
    }

    @Transactional
    fun seed() {
        val now = Instant.now()
        val assetRows = listOf(
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "PIVOT_01", "PIVOT", "IDLE", BigDecimal("8.0")),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "PIVOT_02", "PIVOT", "RUNNING", BigDecimal("7.5")),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-003"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-003"), "DRIP_01", "DRIP", "IDLE", BigDecimal("4.0")),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-004"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "PIVOT_NORTH", "PIVOT", "IDLE", BigDecimal("9.0")),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-005"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-005"), "SPRINKLER_SOUTH", "SPRINKLER", "MAINTENANCE", BigDecimal("5.5")),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-006"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "PIVOT_A", "PIVOT", "IDLE", BigDecimal("8.5")),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-007"), DemoIds.uuid("farm-004"), null, "CENTRAL_PUMP", "PUMP", "IDLE", null),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-008"), DemoIds.uuid("farm-005"), null, "RESERVOIR_01", "RESERVOIR", "IDLE", null),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-009"), DemoIds.uuid("farm-006"), DemoIds.uuid("field-017"), "PIVOT_VV", "PIVOT", "IDLE", BigDecimal("7.8")),
            IrrigationAssetEntity(DemoIds.uuid("irr-asset-010"), DemoIds.uuid("farm-008"), DemoIds.uuid("field-021"), "DRIP_NE", "DRIP", "IDLE", BigDecimal("3.5")),
        )
        assets.saveAll(assetRows)

        val recoRows = listOf(
            IrrigationRecommendationEntity(DemoIds.uuid("irr-reco-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), DemoIds.uuid("irr-asset-001"), BigDecimal("18"), now, now.plus(12, ChronoUnit.HOURS), "WATER_DEFICIT", "OPEN"),
            IrrigationRecommendationEntity(DemoIds.uuid("irr-reco-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), DemoIds.uuid("irr-asset-002"), BigDecimal("12"), now.plus(6, ChronoUnit.HOURS), now.plus(18, ChronoUnit.HOURS), "KEEP_R1_MOISTURE", "OPEN"),
            IrrigationRecommendationEntity(DemoIds.uuid("irr-reco-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), DemoIds.uuid("irr-asset-004"), BigDecimal("22"), now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS), "HIGH_ET", "OPEN"),
            IrrigationRecommendationEntity(DemoIds.uuid("irr-reco-004"), DemoIds.uuid("farm-006"), DemoIds.uuid("field-017"), DemoIds.uuid("irr-asset-009"), BigDecimal("15"), now.plus(4, ChronoUnit.HOURS), now.plus(16, ChronoUnit.HOURS), "BELOW_THRESHOLD", "OPEN"),
        )
        recommendations.saveAll(recoRows)
    }

    private fun IrrigationAssetEntity.toDto() = IrrigationAssetDto(id, farmId, fieldId, name, type, status, capacityMmH)
    private fun IrrigationRecommendationEntity.toDto() =
        IrrigationRecommendationDto(id, farmId, fieldId, assetId, recommendedMm, windowStart, windowEnd, reason, status)
    private fun IrrigationSimulationEntity.toDto() =
        SimulationDto(id, farmId, fieldId, mm, durationH, estimatedCost, waterM3, createdAt)
}

@Service
class IrrigationSeed(private val svc: IrrigationService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedIrrigation() = ApplicationRunner { if (seed) svc.seed() }
}
