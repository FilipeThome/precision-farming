package com.precisionfarming.agronomy.application

import com.precisionfarming.agronomy.domain.LabAdapter
import com.precisionfarming.agronomy.domain.PrescriptionStatus
import com.precisionfarming.agronomy.infrastructure.PrescriptionEntity
import com.precisionfarming.agronomy.infrastructure.PrescriptionJpaRepository
import com.precisionfarming.agronomy.infrastructure.RecommendationEntity
import com.precisionfarming.agronomy.infrastructure.RecommendationJpaRepository
import com.precisionfarming.agronomy.infrastructure.ScoutingEntity
import com.precisionfarming.agronomy.infrastructure.ScoutingJpaRepository
import com.precisionfarming.agronomy.infrastructure.SoilSampleEntity
import com.precisionfarming.agronomy.infrastructure.SoilSampleJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoFieldFarms
import com.precisionfarming.common.concurrency.VirtualJobs
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable

data class ScoutingDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID, val observedAt: Instant,
    val pest: String?, val severity: String, val notes: String?, val status: String,
)
data class CreateScouting(val farmId: UUID, val fieldId: UUID, val pest: String?, val severity: String, val notes: String?)
data class SoilSampleDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID, val sampledAt: Instant,
    val ph: BigDecimal?, val organicMatterPct: BigDecimal?, val pPpm: BigDecimal?, val kPpm: BigDecimal?,
    val labRef: String?, val status: String,
)
data class CreateSoilSample(val farmId: UUID, val fieldId: UUID)
data class RecommendationDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID?, val kind: String, val title: String,
    val summary: String, val priority: String, val status: String, val createdAt: Instant,
)
data class PrescriptionDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID, val product: String, val rate: BigDecimal,
    val unit: String, val status: String, val createdAt: Instant, val approvedAt: Instant?,
)
data class CreatePrescription(val farmId: UUID, val fieldId: UUID, val product: String, val rate: BigDecimal, val unit: String)

@Service
class AgronomyService(
    private val scouting: ScoutingJpaRepository,
    private val soils: SoilSampleJpaRepository,
    private val recommendations: RecommendationJpaRepository,
    private val prescriptions: PrescriptionJpaRepository,
    private val lab: LabAdapter,
) {
    fun listScouting(scope: AccessScope, farmId: UUID?) =
        scouting.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createScouting(scope: AccessScope, cmd: CreateScouting): ScoutingDto {
        scope.requireFarm(cmd.farmId)
        DemoFieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        return scouting.save(
            ScoutingEntity(UUID.randomUUID(), cmd.farmId, cmd.fieldId, Instant.now(), cmd.pest, cmd.severity, cmd.notes, "OPEN"),
        ).toDto()
    }

    fun listSoil(scope: AccessScope, farmId: UUID?) =
        soils.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createSoil(scope: AccessScope, cmd: CreateSoilSample): SoilSampleDto {
        scope.requireFarm(cmd.farmId)
        DemoFieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        val id = UUID.randomUUID()
        val result = lab.analyze(id.toString().take(8))
        return soils.save(
            SoilSampleEntity(
                id, cmd.farmId, cmd.fieldId, Instant.now(),
                result.ph, result.organicMatterPct, result.pPpm, result.kPpm, result.labRef, "ANALYZED",
            ),
        ).toDto()
    }

    fun listRecommendations(scope: AccessScope, farmId: UUID?) =
        recommendations.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun listPrescriptions(scope: AccessScope, farmId: UUID?) =
        prescriptions.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createPrescription(scope: AccessScope, cmd: CreatePrescription): PrescriptionDto {
        scope.requireFarm(cmd.farmId)
        DemoFieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        return prescriptions.save(
            PrescriptionEntity(
                UUID.randomUUID(), cmd.farmId, cmd.fieldId, cmd.product, cmd.rate, cmd.unit,
                PrescriptionStatus.DRAFT.name, Instant.now(), null,
            ),
        ).toDto()
    }

    @Transactional
    fun approvePrescription(scope: AccessScope, id: UUID): PrescriptionDto {
        val e = prescriptions.findById(id).orElseThrow { NotFoundException("PRESCRIPTION_NOT_FOUND", "Prescription not found") }
        scope.requireEntityFarm(e.farmId)
        e.status = PrescriptionStatus.APPROVED.name
        e.approvedAt = Instant.now()
        return prescriptions.save(e).toDto()
    }

    @Transactional
    fun seed() {
        val now = Instant.now()
        val fields = (1..22).map { "field-%03d".format(it) }
        val fieldFarm = mapOf(
            "field-001" to "farm-001", "field-002" to "farm-001", "field-003" to "farm-001", "field-014" to "farm-001",
            "field-004" to "farm-002", "field-005" to "farm-002", "field-015" to "farm-002",
            "field-006" to "farm-003", "field-007" to "farm-003", "field-008" to "farm-003",
            "field-009" to "farm-004", "field-010" to "farm-004", "field-011" to "farm-004",
            "field-012" to "farm-005", "field-013" to "farm-005", "field-016" to "farm-005",
            "field-017" to "farm-006", "field-018" to "farm-006",
            "field-019" to "farm-007", "field-020" to "farm-007",
            "field-021" to "farm-008", "field-022" to "farm-008",
        )
        val scoutRows = VirtualJobs.all(
            (1..40).map { i ->
                Callable {
                    val field = fields[(i - 1) % fields.size]
                    val farm = fieldFarm.getValue(field)
                    ScoutingEntity(
                        DemoIds.uuid("scout-%03d".format(i)), DemoIds.uuid(farm), DemoIds.uuid(field),
                        now.minus(i.toLong(), ChronoUnit.DAYS),
                        listOf("Helicoverpa", "Lagarta", "Ferrugem", "Percevejo", null)[i % 5],
                        listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")[i % 4],
                        "Observação demo $i", "OPEN",
                    )
                }
            },
        )
        val existingScout = scouting.findAllById(scoutRows.map { it.id }).associateBy { it.id }
        val scoutToReplace = scoutRows.filter { desired ->
            val existing = existingScout[desired.id] ?: return@filter false
            existing.farmId != desired.farmId || existing.fieldId != desired.fieldId
        }
        if (scoutToReplace.isNotEmpty()) {
            scouting.deleteAllById(scoutToReplace.map { it.id })
        }
        scouting.saveAll(
            scoutRows.filter { it.id !in existingScout || scoutToReplace.any { r -> r.id == it.id } },
        )

        val soilRows = (1..30).map { i ->
            val field = fields[(i - 1) % fields.size]
            val farm = fieldFarm.getValue(field)
            val labResult = lab.analyze("soil-%03d".format(i))
            SoilSampleEntity(
                DemoIds.uuid("soil-%03d".format(i)), DemoIds.uuid(farm), DemoIds.uuid(field),
                now.minus((i * 3).toLong(), ChronoUnit.DAYS),
                labResult.ph, labResult.organicMatterPct, labResult.pPpm, labResult.kPpm, labResult.labRef, "ANALYZED",
            )
        }
        val existingSoil = soils.findAllById(soilRows.map { it.id }).associateBy { it.id }
        val soilToReplace = soilRows.filter { desired ->
            val existing = existingSoil[desired.id] ?: return@filter false
            existing.farmId != desired.farmId || existing.fieldId != desired.fieldId
        }
        if (soilToReplace.isNotEmpty()) {
            soils.deleteAllById(soilToReplace.map { it.id })
        }
        soils.saveAll(
            soilRows.filter { it.id !in existingSoil || soilToReplace.any { r -> r.id == it.id } },
        )

        val recoRows = listOf(
            RecommendationEntity(DemoIds.uuid("reco-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "FERTILIZER", "Correção de P", "Aplicar MAP em taxa variável", "HIGH", "OPEN", now),
            RecommendationEntity(DemoIds.uuid("reco-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "PROTECTION", "Janela de pulverização", "Aplicar inseticida nas próximas 48h", "CRITICAL", "OPEN", now),
            RecommendationEntity(DemoIds.uuid("reco-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "IRRIGATION", "Déficit hídrico", "Irrigar 18 mm no Talhão Norte", "MEDIUM", "OPEN", now),
            RecommendationEntity(DemoIds.uuid("reco-004"), DemoIds.uuid("farm-003"), null, "SCOUTING", "Monitorar ferrugem", "Intensificar scouting em talhões A", "LOW", "OPEN", now),
        )
        val existingReco = recommendations.findAllById(recoRows.map { it.id }).map { it.id }.toHashSet()
        recommendations.saveAll(recoRows.filter { it.id !in existingReco })
        val rxRows = (1..16).map { i ->
            val field = fields[(i - 1) % fields.size]
            val farm = fieldFarm.getValue(field)
            PrescriptionEntity(
                DemoIds.uuid("rx-%03d".format(i)),
                DemoIds.uuid(farm),
                DemoIds.uuid(field),
                listOf("Glifosato", "MAP", "Ureia", "Inseticida")[i % 4],
                BigDecimal("${1 + i % 5}.${i % 10}"),
                if (i % 2 == 0) "L/ha" else "kg/ha",
                if (i % 3 == 0) PrescriptionStatus.APPROVED.name else PrescriptionStatus.DRAFT.name,
                now.minus(i.toLong(), ChronoUnit.DAYS),
                if (i % 3 == 0) now.minus((i - 1).toLong(), ChronoUnit.DAYS) else null,
            )
        }
        val existingRx = prescriptions.findAllById(rxRows.map { it.id }).map { it.id }.toHashSet()
        prescriptions.saveAll(rxRows.filter { it.id !in existingRx })
    }

    private fun ScoutingEntity.toDto() = ScoutingDto(id, farmId, fieldId, observedAt, pest, severity, notes, status)
    private fun SoilSampleEntity.toDto() = SoilSampleDto(id, farmId, fieldId, sampledAt, ph, organicMatterPct, pPpm, kPpm, labRef, status)
    private fun RecommendationEntity.toDto() = RecommendationDto(id, farmId, fieldId, kind, title, summary, priority, status, createdAt)
    private fun PrescriptionEntity.toDto() = PrescriptionDto(id, farmId, fieldId, product, rate, unit, status, createdAt, approvedAt)
}

@Service
class AgronomySeed(private val svc: AgronomyService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedAgronomy() = ApplicationRunner { if (seed) svc.seed() }
}
