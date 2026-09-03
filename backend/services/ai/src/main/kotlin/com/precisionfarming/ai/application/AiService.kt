package com.precisionfarming.ai.application

import com.precisionfarming.ai.infrastructure.PredictionEntity
import com.precisionfarming.ai.infrastructure.PredictionJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoFieldFarms
import com.precisionfarming.security.DemoMachineFarms
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PredictionDto(
    val id: String, val type: String, val entityId: UUID, val score: Double, val confidence: Double,
    val horizonHours: Int?, val model: String, val modelVersion: String, val generatedAt: Instant,
    val explanation: List<String>, val demo: Boolean = true,
)

@Service
class AiService(private val repo: PredictionJpaRepository) {
    fun insights(scope: AccessScope, farmId: UUID?): List<PredictionDto> {
        val farms = scope.resolveFarms(farmId)
        return repo.findByFarmIdIn(farms).map { it.toDto() }
    }

    fun predictions(scope: AccessScope, fieldId: UUID): List<PredictionDto> {
        DemoFieldFarms.requireField(scope, fieldId)
        return repo.findByEntityId(fieldId).map { it.toDto() }
    }

    fun machineRisk(scope: AccessScope, machineId: UUID): List<PredictionDto> {
        DemoMachineFarms.requireMachine(scope, machineId)
        return repo.findByEntityId(machineId).map { it.toDto() }
    }

    fun feedback(scope: AccessScope, id: UUID): Map<String, Any> {
        val e = repo.findById(id).orElseThrow { NotFoundException("PREDICTION_NOT_FOUND", "Prediction not found") }
        val farmId = e.farmId
            ?: DemoFieldFarms.farmId(e.entityId)
            ?: DemoMachineFarms.farmId(e.entityId)
            ?: throw NotFoundException("PREDICTION_NOT_FOUND", "Prediction farm unknown")
        scope.requireEntityFarm(farmId)
        return mapOf("id" to id, "status" to "recorded")
    }

    @Transactional
    fun seed() {
        val now = Instant.now()
        val rows = listOf(
            PredictionEntity(
                DemoIds.uuid("prediction-001"), "MACHINE_FAILURE_RISK", "MACHINE", DemoIds.uuid("machine-001"),
                BigDecimal("0.72"), BigDecimal("0.81"), "demo-gradient-baseline", "0.1.0",
                Instant.parse("2026-08-31T10:00:00Z"), "HIGH_ENGINE_HOURS|INCREASING_TEMP_VARIANCE|RECENT_DIAGNOSTIC",
                72, DemoIds.uuid("farm-001"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-002"), "YIELD_FORECAST", "FIELD", DemoIds.uuid("field-001"),
                BigDecimal("0.64"), BigDecimal("0.70"), "demo-gradient-baseline", "0.1.0",
                now, "SOY_BASELINE_BOA_VISTA|SEASON_AVG_WEATHER", 720, DemoIds.uuid("farm-001"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-003"), "OPERATIONAL_DELAY_RISK", "OPERATION", DemoIds.uuid("op-002"),
                BigDecimal("0.55"), BigDecimal("0.66"), "demo-gradient-baseline", "0.1.0",
                now, "SPRAY_BACKLOG|UNFAVORABLE_WEATHER_WINDOW", 48, DemoIds.uuid("farm-001"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-004"), "MACHINE_FAILURE_RISK", "MACHINE", DemoIds.uuid("machine-004"),
                BigDecimal("0.68"), BigDecimal("0.74"), "demo-gradient-baseline", "0.1.0",
                now, "OVERDUE_WO|HIGH_ENGINE_HOURS", 96, DemoIds.uuid("farm-002"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-005"), "YIELD_FORECAST", "FIELD", DemoIds.uuid("field-006"),
                BigDecimal("0.71"), BigDecimal("0.78"), "demo-gradient-baseline", "0.1.0",
                now, "FIELD_A_GOOD_MOISTURE|STABLE_NDVI", 720, DemoIds.uuid("farm-003"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-006"), "OPERATIONAL_DELAY_RISK", "OPERATION", DemoIds.uuid("op-010"),
                BigDecimal("0.61"), BigDecimal("0.69"), "demo-gradient-baseline", "0.1.0",
                now, "PLANTING_QUEUE|LIMITED_FLEET", 36, DemoIds.uuid("farm-004"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-007"), "WEATHER_RISK", "FARM", DemoIds.uuid("farm-005"),
                BigDecimal("0.48"), BigDecimal("0.62"), "demo-gradient-baseline", "0.1.0",
                now, "RAIN_48H|WIND_ABOVE_THRESHOLD", 48, DemoIds.uuid("farm-005"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-008"), "PEST_PRESSURE", "FIELD", DemoIds.uuid("field-017"),
                BigDecimal("0.52"), BigDecimal("0.60"), "demo-gradient-baseline", "0.1.0",
                now, "CATERPILLAR_HISTORY|FAVORABLE_TEMP", 120, DemoIds.uuid("farm-006"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-009"), "MACHINE_FAILURE_RISK", "MACHINE", DemoIds.uuid("machine-011"),
                BigDecimal("0.77"), BigDecimal("0.83"), "demo-gradient-baseline", "0.1.0",
                now, "STATUS_MAINTENANCE|ABNORMAL_VIBRATION", 24, DemoIds.uuid("farm-007"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-010"), "YIELD_FORECAST", "FIELD", DemoIds.uuid("field-021"),
                BigDecimal("0.58"), BigDecimal("0.67"), "demo-gradient-baseline", "0.1.0",
                now, "EARLY_SEASON|MEDIUM_P_SOIL", 720, DemoIds.uuid("farm-008"),
            ),
        )
        repo.saveAll(rows)
    }

    private fun PredictionEntity.toDto() = PredictionDto(
        id.toString(), type, entityId, score.toDouble(), confidence.toDouble(), horizonHours, model, modelVersion, generatedAt,
        explanation.split("|"), true,
    )
}

@Service
class AiSeed(private val svc: AiService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedAi() = ApplicationRunner { if (seed) svc.seed() }
}
