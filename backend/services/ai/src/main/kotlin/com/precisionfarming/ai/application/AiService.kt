package com.precisionfarming.ai.application

import com.precisionfarming.ai.infrastructure.PredictionEntity
import com.precisionfarming.ai.infrastructure.PredictionJpaRepository
import com.precisionfarming.common.DemoIds
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
    fun insights(farmId: UUID?) = repo.findAll().map { it.toDto() }
    fun predictions(fieldId: UUID) = repo.findByEntityId(fieldId).map { it.toDto() }
    fun machineRisk(machineId: UUID) = repo.findByEntityId(machineId).map { it.toDto() }
    fun feedback(id: UUID) = mapOf("id" to id, "status" to "recorded")

    @Transactional
    fun seed() {
        if (repo.existsById(DemoIds.uuid("prediction-001"))) return
        repo.saveAll(
            listOf(
                PredictionEntity(DemoIds.uuid("prediction-001"), "MACHINE_FAILURE_RISK", "MACHINE", DemoIds.uuid("machine-001"), BigDecimal("0.72"), BigDecimal("0.81"), "demo-gradient-baseline", "0.1.0", Instant.parse("2026-08-31T10:00:00Z"), "High engine hours|Increasing temperature variance|Recent diagnostic event", 72),
                PredictionEntity(DemoIds.uuid("prediction-002"), "YIELD_FORECAST", "FIELD", DemoIds.uuid("field-001"), BigDecimal("0.64"), BigDecimal("0.70"), "demo-gradient-baseline", "0.1.0", Instant.now(), "Baseline soja Boa Vista|Clima na média da safra", 720),
                PredictionEntity(DemoIds.uuid("prediction-003"), "OPERATIONAL_DELAY_RISK", "OPERATION", DemoIds.uuid("op-002"), BigDecimal("0.55"), BigDecimal("0.66"), "demo-gradient-baseline", "0.1.0", Instant.now(), "Backlog de pulverização|Janela climática desfavorável", 48),
            ),
        )
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
