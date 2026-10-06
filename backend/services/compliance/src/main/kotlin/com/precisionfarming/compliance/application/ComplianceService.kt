package com.precisionfarming.compliance.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.compliance.domain.DEFORESTATION_CUTOFF
import com.precisionfarming.security.AccessScope
import com.precisionfarming.compliance.infrastructure.CreditDossierEntity
import com.precisionfarming.compliance.infrastructure.CreditDossierJpaRepository
import com.precisionfarming.compliance.infrastructure.EsgMetricEntity
import com.precisionfarming.compliance.infrastructure.EsgMetricJpaRepository
import com.precisionfarming.compliance.infrastructure.EvidencePackEntity
import com.precisionfarming.compliance.infrastructure.EvidencePackJpaRepository
import com.precisionfarming.compliance.infrastructure.TraceabilityEntity
import com.precisionfarming.compliance.infrastructure.TraceabilityJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
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
data class EvidencePackDto(
    val lotCode: String,
    val farmId: UUID,
    val farmName: String,
    val fieldId: UUID,
    val fieldName: String,
    val polygonGeoJson: String,
    val inputRefs: List<String>,
    val receituarioNumber: String?,
    val activeIngredient: String?,
    val moaGroup: String?,
    val responsibleTechCpf: String?,
    val phiDays: Int?,
    val deforestationCutoffDate: LocalDate,
    val embargoed: Boolean,
    val carStatus: String,
    val simulation: Boolean = true,
)
data class CreditDossierDto(
    val farmId: UUID,
    val carCode: String,
    val carStatus: String,
    val embargoed: Boolean,
    val deforestationCutoffDate: LocalDate,
    val deforestationClear: Boolean,
    val zarcCompliant: Boolean,
    val remoteSensingNote: String?,
    val simulation: Boolean = true,
)

@Service
class ComplianceService(
    private val traces: TraceabilityJpaRepository,
    private val esg: EsgMetricJpaRepository,
    private val evidence: EvidencePackJpaRepository,
    private val dossiers: CreditDossierJpaRepository,
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

    fun getEvidencePack(scope: AccessScope, lotCode: String): EvidencePackDto {
        val e = evidence.findById(lotCode).orElseThrow {
            NotFoundException("EVIDENCE_PACK_NOT_FOUND", "Evidence pack not found")
        }
        scope.requireFarmRead(e.farmId, "EVIDENCE_PACK_NOT_FOUND", "Evidence pack not found")
        return e.toDto()
    }

    fun getCreditDossier(scope: AccessScope, farmId: UUID): CreditDossierDto {
        val e = dossiers.findById(farmId).orElseThrow {
            NotFoundException("CREDIT_DOSSIER_NOT_FOUND", "Credit dossier not found")
        }
        scope.requireFarmRead(e.farmId, "CREDIT_DOSSIER_NOT_FOUND", "Credit dossier not found")
        return e.toDto()
    }

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
        val existingTraces = traces.findAllById(traceRows.map { it.id }).map { it.id }.toSet()
        traces.saveAll(traceRows.filter { it.id !in existingTraces })

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
        val existingEsg = esg.findAllById(esgRows.map { it.id }).map { it.id }.toSet()
        esg.saveAll(esgRows.filter { it.id !in existingEsg })

        // Snapshot only — no join to farm_db. Polygon near -54.57,-19.39 (field-001 seed).
        val polygon = """{"type":"Polygon","coordinates":[[[-54.575,-19.385],[-54.565,-19.385],[-54.565,-19.395],[-54.575,-19.395],[-54.575,-19.385]]]}"""
        val evidenceRows = listOf(
            EvidencePackEntity(
                lotCode = "LOT-BV-001",
                farmId = DemoIds.uuid("farm-001"),
                farmName = "Boa Vista",
                fieldId = DemoIds.uuid("field-001"),
                fieldName = "Field 001",
                polygonGeoJson = polygon,
                inputRefs = "item-001,rx-spot-001",
                receituarioNumber = "REC-DEMO-001",
                activeIngredient = "GLYPHOSATE",
                moaGroup = "G",
                responsibleTechCpf = "00000000191", // DEMO_CPF — not a real person
                phiDays = 7,
                deforestationCutoffDate = DEFORESTATION_CUTOFF,
                embargoed = false,
                carStatus = "ATIVO",
            ),
        )
        val existingEvidence = evidence.findAllById(evidenceRows.map { it.lotCode }).map { it.lotCode }.toSet()
        evidence.saveAll(evidenceRows.filter { it.lotCode !in existingEvidence })

        val dossierRows = listOf(
            CreditDossierEntity(
                farmId = DemoIds.uuid("farm-001"),
                carCode = "MS-1234567",
                carStatus = "ATIVO",
                embargoed = false,
                deforestationCutoffDate = DEFORESTATION_CUTOFF,
                deforestationClear = true,
                zarcCompliant = true,
                remoteSensingNote = "No deforestation alerts after cutoff (simulation)",
            ),
            CreditDossierEntity(
                farmId = DemoIds.uuid("farm-003"),
                carCode = "MS-7654321",
                carStatus = "ATIVO",
                embargoed = true,
                deforestationCutoffDate = DEFORESTATION_CUTOFF,
                deforestationClear = false,
                zarcCompliant = true,
                remoteSensingNote = "Embargoed parcel near CAR polygon (simulation)",
            ),
        )
        val existingDossiers = dossiers.findAllById(dossierRows.map { it.farmId }).map { it.farmId }.toSet()
        dossiers.saveAll(dossierRows.filter { it.farmId !in existingDossiers })
    }

    private fun TraceabilityEntity.toDto() = TraceabilityDto(id, farmId, fieldId, lotCode, crop, eventType, summary, occurredAt)
    private fun EsgMetricEntity.toDto() = EsgMetricDto(id, farmId, metric, value, unit, periodLabel, score)
    private fun EvidencePackEntity.toDto() = EvidencePackDto(
        lotCode = lotCode,
        farmId = farmId,
        farmName = farmName,
        fieldId = fieldId,
        fieldName = fieldName,
        polygonGeoJson = polygonGeoJson,
        inputRefs = inputRefs.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        receituarioNumber = receituarioNumber,
        activeIngredient = activeIngredient,
        moaGroup = moaGroup,
        responsibleTechCpf = responsibleTechCpf,
        phiDays = phiDays,
        deforestationCutoffDate = deforestationCutoffDate,
        embargoed = embargoed,
        carStatus = carStatus,
        simulation = true,
    )
    private fun CreditDossierEntity.toDto() = CreditDossierDto(
        farmId = farmId,
        carCode = carCode,
        carStatus = carStatus,
        embargoed = embargoed,
        deforestationCutoffDate = deforestationCutoffDate,
        deforestationClear = deforestationClear,
        zarcCompliant = zarcCompliant,
        remoteSensingNote = remoteSensingNote,
        simulation = true,
    )
}

@Service
class ComplianceSeed(private val svc: ComplianceService, private val gate: com.precisionfarming.security.DemoSeedGate) {
    @Bean fun seedCompliance() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
