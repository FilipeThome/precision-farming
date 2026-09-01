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
        val rows = listOf(
            PredictionEntity(
                DemoIds.uuid("prediction-001"), "MACHINE_FAILURE_RISK", "MACHINE", DemoIds.uuid("machine-001"),
                BigDecimal("0.72"), BigDecimal("0.81"), "demo-gradient-baseline", "0.1.0",
                Instant.parse("2026-08-31T10:00:00Z"), "High engine hours|Increasing temperature variance|Recent diagnostic event",
                72, DemoIds.uuid("farm-001"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-002"), "YIELD_FORECAST", "FIELD", DemoIds.uuid("field-001"),
                BigDecimal("0.64"), BigDecimal("0.70"), "demo-gradient-baseline", "0.1.0",
                Instant.now(), "Baseline soja Boa Vista|Clima na média da safra", 720, DemoIds.uuid("farm-001"),
            ),
            PredictionEntity(
                DemoIds.uuid("prediction-003"), "OPERATIONAL_DELAY_RISK", "OPERATION", DemoIds.uuid("op-002"),
                BigDecimal("0.55"), BigDecimal("0.66"), "demo-gradient-baseline", "0.1.0",
                Instant.now(), "Backlog de pulverização|Janela climática desfavorável", 48, DemoIds.uuid("farm-001"),
            ),
        )
        if (!repo.existsById(DemoIds.uuid("prediction-001"))) {
            repo.saveAll(rows)
            return
        }
        rows.forEach { demo ->
            repo.findById(demo.id).ifPresent { existing ->
                if (existing.farmId == null) {
                    existing.farmId = demo.farmId
                    repo.save(existing)
                }
            }
        }
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
