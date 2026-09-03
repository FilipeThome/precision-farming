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
        val now = Instant.now()
        val traceRows = listOf(
            TraceabilityEntity(DemoIds.uuid("trace-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "LOT-BV-001", "SOY", "HARVEST", "HARVEST_FIELD01_SILO01", now.minus(2, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "LOT-BV-001", "SOY", "APPLICATION", "INSECTICIDE_APPLICATION_LOGGED", now.minus(20, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-003"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "LOT-BV-002", "CORN", "PLANTING", "PLANTING_SEED_LOT", now.minus(60, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-004"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "LOT-SH-010", "SOY", "TRANSPORT", "LOAD_003_DISPATCHED", now.minus(1, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-005"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "LOT-HZ-020", "SOY", "STORAGE", "LOT_RECEIVED_SILO_A", now.minus(5, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-006"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "LOT-BV-001", "SOY", "TRANSPORT", "LOAD_002_IN_TRANSIT_DRY_PORT", now.minus(1, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-007"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "LOT-BV-001", "SOY", "STORAGE", "LOT_CONSOLIDATED_SILO01", now.minus(12, ChronoUnit.HOURS)),
            TraceabilityEntity(DemoIds.uuid("trace-008"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "LOT-HZ-020", "SOY", "HARVEST", "PARTIAL_HARVEST_FIELD_A", now.minus(8, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-009"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "LOT-HZ-020", "SOY", "APPLICATION", "PREHARVEST_FUNGICIDE", now.minus(25, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-010"), DemoIds.uuid("farm-007"), DemoIds.uuid("field-019"), "LOT-ES-030", "SOY", "HARVEST", "HARVEST_FIELD_ES_NORTH", now.minus(3, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-011"), DemoIds.uuid("farm-007"), DemoIds.uuid("field-019"), "LOT-ES-030", "SOY", "TRANSPORT", "LOAD_008_DELIVERED", now.minus(1, ChronoUnit.DAYS)),
            TraceabilityEntity(DemoIds.uuid("trace-012"), DemoIds.uuid("farm-004"), DemoIds.uuid("field-009"), "LOT-PR-040", "SOY", "PLANTING", "PLANTING_EAST_FIELD", now.minus(90, ChronoUnit.DAYS)),
        )
        traces.saveAll(traceRows)

        val esgRows = listOf(
            EsgMetricEntity(DemoIds.uuid("esg-001"), DemoIds.uuid("farm-001"), "CO2E_PER_HA", BigDecimal("1.85"), "tCO2e/ha", "2025/26", BigDecimal("78")),
            EsgMetricEntity(DemoIds.uuid("esg-002"), DemoIds.uuid("farm-001"), "WATER_PER_TON", BigDecimal("420"), "m3/t", "2025/26", BigDecimal("71")),
            EsgMetricEntity(DemoIds.uuid("esg-003"), DemoIds.uuid("farm-001"), "SOIL_HEALTH", BigDecimal("7.2"), "index", "2025/26", BigDecimal("82")),
            EsgMetricEntity(DemoIds.uuid("esg-004"), DemoIds.uuid("farm-002"), "CO2E_PER_HA", BigDecimal("2.10"), "tCO2e/ha", "2025/26", BigDecimal("69")),
            EsgMetricEntity(DemoIds.uuid("esg-005"), DemoIds.uuid("farm-003"), "WATER_PER_TON", BigDecimal("390"), "m3/t", "2025/26", BigDecimal("75")),
            EsgMetricEntity(DemoIds.uuid("esg-006"), DemoIds.uuid("farm-004"), "CO2E_PER_HA", BigDecimal("1.95"), "tCO2e/ha", "2025/26", BigDecimal("74")),
            EsgMetricEntity(DemoIds.uuid("esg-007"), DemoIds.uuid("farm-006"), "SOIL_HEALTH", BigDecimal("6.8"), "index", "2025/26", BigDecimal("70")),
            EsgMetricEntity(DemoIds.uuid("esg-008"), DemoIds.uuid("farm-008"), "WATER_PER_TON", BigDecimal("405"), "m3/t", "2025/26", BigDecimal("73")),
        )
        esg.saveAll(esgRows)
    }

    private fun TraceabilityEntity.toDto() = TraceabilityDto(id, farmId, fieldId, lotCode, crop, eventType, summary, occurredAt)
    private fun EsgMetricEntity.toDto() = EsgMetricDto(id, farmId, metric, value, unit, periodLabel, score)
}

@Service
class ComplianceSeed(private val svc: ComplianceService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedCompliance() = ApplicationRunner { if (seed) svc.seed() }
}
