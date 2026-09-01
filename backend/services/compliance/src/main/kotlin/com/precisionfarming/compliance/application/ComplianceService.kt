package com.precisionfarming.compliance.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.compliance.infrastructure.EsgMetricEntity
import com.precisionfarming.compliance.infrastructure.EsgMetricJpaRepository
import com.precisionfarming.compliance.infrastructure.TraceabilityEntity
import com.precisionfarming.compliance.infrastructure.TraceabilityJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class TraceabilityDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID?, val lotCode: String, val crop: String,
    val eventType: String, val summary: String, val occurredAt: Instant,
)
data class EsgMetricDto(
    val id: UUID, val farmId: UUID, val metric: String, val value: BigDecimal, val unit: String,
    val periodLabel: String, val score: BigDecimal?,
)

@Service
class ComplianceService(
    private val traces: TraceabilityJpaRepository,
    private val esg: EsgMetricJpaRepository,
) {
    fun listTraceability(scope: AccessScope, farmId: UUID?) =
        traces.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun getTraceability(scope: AccessScope, id: UUID): TraceabilityDto {
        val e = traces.findById(id).orElseThrow { NotFoundException("TRACE_NOT_FOUND", "Traceability record not found") }
        scope.requireEntityFarm(e.farmId)
        return e.toDto()
    }

    fun listEsg(scope: AccessScope, farmId: UUID?) =
        esg.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun seed() {
        if (traces.existsById(DemoIds.uuid("trace-001"))) return
        val now = Instant.now()
        traces.saveAll(
            listOf(
                TraceabilityEntity(DemoIds.uuid("trace-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "LOT-BV-001", "Soja", "HARVEST", "Colheita talhão 01 vinculada ao silo 01", now.minus(2, ChronoUnit.DAYS)),
                TraceabilityEntity(DemoIds.uuid("trace-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "LOT-BV-001", "Soja", "APPLICATION", "Aplicação de inseticida registrada", now.minus(20, ChronoUnit.DAYS)),
                TraceabilityEntity(DemoIds.uuid("trace-003"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "LOT-BV-002", "Milho", "PLANTING", "Plantio com lote de semente item-003", now.minus(60, ChronoUnit.DAYS)),
                TraceabilityEntity(DemoIds.uuid("trace-004"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "LOT-SH-010", "Soja", "TRANSPORT", "Carga load-003 despachada", now.minus(1, ChronoUnit.DAYS)),
                TraceabilityEntity(DemoIds.uuid("trace-005"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "LOT-HZ-020", "Soja", "STORAGE", "Lote recebido no silo A", now.minus(5, ChronoUnit.DAYS)),
            ),
        )
        esg.saveAll(
            listOf(
                EsgMetricEntity(DemoIds.uuid("esg-001"), DemoIds.uuid("farm-001"), "CO2E_PER_HA", BigDecimal("1.85"), "tCO2e/ha", "2025/26", BigDecimal("78")),
                EsgMetricEntity(DemoIds.uuid("esg-002"), DemoIds.uuid("farm-001"), "WATER_PER_TON", BigDecimal("420"), "m3/t", "2025/26", BigDecimal("71")),
                EsgMetricEntity(DemoIds.uuid("esg-003"), DemoIds.uuid("farm-001"), "SOIL_HEALTH", BigDecimal("7.2"), "index", "2025/26", BigDecimal("82")),
                EsgMetricEntity(DemoIds.uuid("esg-004"), DemoIds.uuid("farm-002"), "CO2E_PER_HA", BigDecimal("2.10"), "tCO2e/ha", "2025/26", BigDecimal("69")),
                EsgMetricEntity(DemoIds.uuid("esg-005"), DemoIds.uuid("farm-003"), "WATER_PER_TON", BigDecimal("390"), "m3/t", "2025/26", BigDecimal("75")),
            ),
        )
    }

    private fun TraceabilityEntity.toDto() = TraceabilityDto(id, farmId, fieldId, lotCode, crop, eventType, summary, occurredAt)
    private fun EsgMetricEntity.toDto() = EsgMetricDto(id, farmId, metric, value, unit, periodLabel, score)
}

@Service
class ComplianceSeed(private val svc: ComplianceService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedCompliance() = ApplicationRunner { if (seed) svc.seed() }
}
