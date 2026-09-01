package com.precisionfarming.irrigation.application

import com.precisionfarming.common.DemoIds
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
    fun listAssets(farmId: UUID?) = (farmId?.let { assets.findByFarmId(it) } ?: assets.findAll()).map { it.toDto() }
    fun listRecommendations(farmId: UUID?) =
        (farmId?.let { recommendations.findByFarmId(it) } ?: recommendations.findAll()).map { it.toDto() }

    @Transactional
    fun simulate(cmd: SimulateRequest): SimulationDto {
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
        if (assets.existsById(DemoIds.uuid("irr-asset-001"))) return
        val now = Instant.now()
        assets.saveAll(
            listOf(
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "Pivô 01", "PIVOT", "IDLE", BigDecimal("8.0")),
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "Pivô 02", "PIVOT", "RUNNING", BigDecimal("7.5")),
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-003"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-003"), "Gotejo 01", "DRIP", "IDLE", BigDecimal("4.0")),
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-004"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "Pivô Norte", "PIVOT", "IDLE", BigDecimal("9.0")),
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-005"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-005"), "Aspersão Sul", "SPRINKLER", "MAINTENANCE", BigDecimal("5.5")),
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-006"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "Pivô A", "PIVOT", "IDLE", BigDecimal("8.5")),
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-007"), DemoIds.uuid("farm-004"), null, "Bomba Central", "PUMP", "IDLE", null),
                IrrigationAssetEntity(DemoIds.uuid("irr-asset-008"), DemoIds.uuid("farm-005"), null, "Reservatório", "RESERVOIR", "IDLE", null),
            ),
        )
        recommendations.saveAll(
            listOf(
                IrrigationRecommendationEntity(DemoIds.uuid("irr-reco-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), DemoIds.uuid("irr-asset-001"), BigDecimal("18"), now, now.plus(12, ChronoUnit.HOURS), "Déficit hídrico estimado", "OPEN"),
                IrrigationRecommendationEntity(DemoIds.uuid("irr-reco-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), DemoIds.uuid("irr-asset-002"), BigDecimal("12"), now.plus(6, ChronoUnit.HOURS), now.plus(18, ChronoUnit.HOURS), "Manter umidade no estágio R1", "OPEN"),
                IrrigationRecommendationEntity(DemoIds.uuid("irr-reco-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), DemoIds.uuid("irr-asset-004"), BigDecimal("22"), now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS), "Evapotranspiração alta", "OPEN"),
            ),
        )
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
