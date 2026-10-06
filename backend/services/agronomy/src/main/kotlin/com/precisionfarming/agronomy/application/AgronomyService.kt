package com.precisionfarming.agronomy.application

import com.precisionfarming.agronomy.domain.DemoFieldAreas
import com.precisionfarming.agronomy.domain.LabAdapter
import com.precisionfarming.agronomy.domain.PrescriptionMode
import com.precisionfarming.agronomy.domain.PrescriptionStatus
import com.precisionfarming.agronomy.domain.moaRotationWarning
import com.precisionfarming.agronomy.domain.requireDraftForTransition
import com.precisionfarming.agronomy.domain.requirePlannedDose
import com.precisionfarming.agronomy.domain.resolveTreatedFraction
import com.precisionfarming.agronomy.domain.spraySavings
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
import com.precisionfarming.security.FieldFarmGuard
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
    val id: UUID,
    val farmId: UUID,
    val fieldId: UUID,
    val product: String,
    val mode: String,
    val treatedFraction: BigDecimal,
    val plannedDose: BigDecimal,
    val unit: String,
    val activeIngredient: String?,
    val moaGroup: String?,
    val receituarioNumber: String?,
    val responsibleTechCpf: String?,
    val phiDays: Int?,
    val reentryHours: Int?,
    val fieldAreaHa: BigDecimal?,
    val status: String,
    val createdAt: Instant,
    val approvedAt: Instant?,
)
data class CreatePrescription(
    val farmId: UUID,
    val fieldId: UUID,
    val product: String,
    val plannedDose: BigDecimal,
    val unit: String,
    val mode: String? = null,
    val treatedFraction: BigDecimal? = null,
    val activeIngredient: String? = null,
    val moaGroup: String? = null,
    val receituarioNumber: String? = null,
    val responsibleTechCpf: String? = null,
    val phiDays: Int? = null,
    val reentryHours: Int? = null,
)
data class SpraySavingsDto(
    val prescriptionId: UUID,
    val fieldId: UUID,
    val mode: String,
    val fieldAreaHa: BigDecimal,
    val treatedHa: BigDecimal,
    val fullRateHa: BigDecimal,
    val litersFullRate: BigDecimal,
    val litersSpot: BigDecimal,
    val litersAvoided: BigDecimal,
    val litersPerHa: BigDecimal,
    val unit: String,
    val simulation: Boolean = true,
)
data class MoaRotationDto(
    val fieldId: UUID,
    val moaGroup: String?,
    val warning: Boolean,
    val prescriptionIds: List<UUID>,
    val simulation: Boolean = true,
)

@Service
class AgronomyService(
    private val scouting: ScoutingJpaRepository,
    private val soils: SoilSampleJpaRepository,
    private val recommendations: RecommendationJpaRepository,
    private val prescriptions: PrescriptionJpaRepository,
    private val lab: LabAdapter,
    private val fieldFarms: FieldFarmGuard,
) {
    fun listScouting(scope: AccessScope, farmId: UUID?) =
        scouting.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createScouting(scope: AccessScope, cmd: CreateScouting): ScoutingDto {
        scope.requireFarm(cmd.farmId)
        fieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        return scouting.save(
            ScoutingEntity(UUID.randomUUID(), cmd.farmId, cmd.fieldId, Instant.now(), cmd.pest, cmd.severity, cmd.notes, "OPEN"),
        ).toDto()
    }

    fun listSoil(scope: AccessScope, farmId: UUID?) =
        soils.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createSoil(scope: AccessScope, cmd: CreateSoilSample): SoilSampleDto {
        scope.requireFarm(cmd.farmId)
        fieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
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

    fun getPrescription(scope: AccessScope, id: UUID): PrescriptionDto {
        val e = prescriptions.findById(id).orElseThrow {
            NotFoundException("PRESCRIPTION_NOT_FOUND", "Prescription not found")
        }
        scope.requireFarmRead(e.farmId, "PRESCRIPTION_NOT_FOUND", "Prescription not found")
        return e.toDto()
    }

    @Transactional
    fun createPrescription(scope: AccessScope, cmd: CreatePrescription): PrescriptionDto {
        scope.requireFarm(cmd.farmId)
        fieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        requirePlannedDose(cmd.plannedDose)
        val mode = PrescriptionMode.valueOf((cmd.mode ?: PrescriptionMode.BROADCAST.name).uppercase())
        val fraction = resolveTreatedFraction(mode, cmd.treatedFraction)
        val area = DemoFieldAreas.forFieldId(cmd.fieldId)
        val ingredient = cmd.activeIngredient ?: cmd.product
        return prescriptions.save(
            PrescriptionEntity(
                id = UUID.randomUUID(),
                farmId = cmd.farmId,
                fieldId = cmd.fieldId,
                product = cmd.product,
                rate = cmd.plannedDose,
                unit = cmd.unit,
                status = PrescriptionStatus.DRAFT.name,
                createdAt = Instant.now(),
                approvedAt = null,
                mode = mode.name,
                treatedFraction = fraction,
                activeIngredient = ingredient,
                moaGroup = cmd.moaGroup,
                receituarioNumber = cmd.receituarioNumber,
                responsibleTechCpf = cmd.responsibleTechCpf,
                phiDays = cmd.phiDays,
                reentryHours = cmd.reentryHours,
                fieldAreaHa = area,
            ),
        ).toDto()
    }

    @Transactional
    fun approvePrescription(scope: AccessScope, id: UUID): PrescriptionDto {
        val e = prescriptions.findById(id).orElseThrow {
            NotFoundException("PRESCRIPTION_NOT_FOUND", "Prescription not found")
        }
        scope.requireEntityFarm(e.farmId)
        requireDraftForTransition(e.status)
        e.status = PrescriptionStatus.APPROVED.name
        e.approvedAt = Instant.now()
        return prescriptions.save(e).toDto()
    }

    @Transactional
    fun rejectPrescription(scope: AccessScope, id: UUID): PrescriptionDto {
        val e = prescriptions.findById(id).orElseThrow {
            NotFoundException("PRESCRIPTION_NOT_FOUND", "Prescription not found")
        }
        scope.requireEntityFarm(e.farmId)
        requireDraftForTransition(e.status)
        e.status = PrescriptionStatus.REJECTED.name
        e.approvedAt = null
        return prescriptions.save(e).toDto()
    }

    fun spraySavings(scope: AccessScope, id: UUID): SpraySavingsDto {
        val e = prescriptions.findById(id).orElseThrow {
            NotFoundException("PRESCRIPTION_NOT_FOUND", "Prescription not found")
        }
        scope.requireFarmRead(e.farmId, "PRESCRIPTION_NOT_FOUND", "Prescription not found")
        val area = e.fieldAreaHa ?: DemoFieldAreas.forFieldId(e.fieldId) ?: BigDecimal.ZERO
        val savings = spraySavings(area, e.treatedFraction, e.rate)
        return SpraySavingsDto(
            prescriptionId = e.id,
            fieldId = e.fieldId,
            mode = e.mode,
            fieldAreaHa = savings.fieldAreaHa,
            treatedHa = savings.treatedHa,
            fullRateHa = savings.fullRateHa,
            litersFullRate = savings.litersFullRate,
            litersSpot = savings.litersSpot,
            litersAvoided = savings.litersAvoided,
            litersPerHa = savings.litersPerHa,
            unit = e.unit,
            simulation = true,
        )
    }

    fun moaRotation(scope: AccessScope, fieldId: UUID): MoaRotationDto {
        val rows = prescriptions.findByFieldId(fieldId)
        if (rows.isEmpty()) {
            throw NotFoundException("PRESCRIPTION_NOT_FOUND", "No prescriptions for field")
        }
        val farmId = rows.first().farmId
        scope.requireFarmRead(farmId, "PRESCRIPTION_NOT_FOUND", "No prescriptions for field")
        val result = moaRotationWarning(rows.map { it.id to it.moaGroup })
        return MoaRotationDto(
            fieldId = fieldId,
            moaGroup = result.moaGroup,
            warning = result.warning,
            prescriptionIds = result.prescriptionIds,
            simulation = true,
        )
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
        insertMissingScouting(now, fields, fieldFarm)
        insertMissingSoil(now, fields, fieldFarm)
        insertMissingRecommendations(now)
        insertMissingPrescriptions(now, fields, fieldFarm)
    }

    private fun insertMissingScouting(now: Instant, fields: List<String>, fieldFarm: Map<String, String>) {
        val scoutRows = VirtualJobs.all(
            (1..40).map { i ->
                Callable {
                    val field = fields[(i - 1) % fields.size]
                    val farm = fieldFarm.getValue(field)
                    ScoutingEntity(
                        DemoIds.uuid("scout-%03d".format(i)), DemoIds.uuid(farm), DemoIds.uuid(field),
                        now.minus(i.toLong(), ChronoUnit.DAYS),
                        listOf("HELICOVERPA", "CATERPILLAR", "RUST", "STINK_BUG", "HEALTHY")[i % 5],
                        listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")[i % 4],
                        "DEMO_SCOUT_$i", "OPEN",
                    )
                }
            },
        )
        val existing = scouting.findAllById(scoutRows.map { it.id }).map { it.id }.toSet()
        scouting.saveAll(scoutRows.filter { it.id !in existing })
    }

    private fun insertMissingSoil(now: Instant, fields: List<String>, fieldFarm: Map<String, String>) {
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
        val existing = soils.findAllById(soilRows.map { it.id }).map { it.id }.toSet()
        soils.saveAll(soilRows.filter { it.id !in existing })
    }

    private fun insertMissingRecommendations(now: Instant) {
        val recoRows = listOf(
            RecommendationEntity(DemoIds.uuid("reco-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "FERTILIZER", "P_CORRECTION", "APPLY_MAP_VR", "HIGH", "OPEN", now),
            RecommendationEntity(DemoIds.uuid("reco-002"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-002"), "PROTECTION", "SPRAY_WINDOW_REC", "APPLY_INSECTICIDE_48H", "CRITICAL", "OPEN", now),
            RecommendationEntity(DemoIds.uuid("reco-003"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "IRRIGATION", "WATER_DEFICIT", "IRRIGATE_NORTH", "MEDIUM", "OPEN", now),
            RecommendationEntity(DemoIds.uuid("reco-004"), DemoIds.uuid("farm-003"), null, "SCOUTING", "MONITOR_RUST", "INTENSIFY_SCOUTING_A", "LOW", "OPEN", now),
        )
        val existing = recommendations.findAllById(recoRows.map { it.id }).map { it.id }.toSet()
        recommendations.saveAll(recoRows.filter { it.id !in existing })
    }

    private fun insertMissingPrescriptions(now: Instant, fields: List<String>, fieldFarm: Map<String, String>) {
        val baseRows = (1..16).map { i ->
            val field = fields[(i - 1) % fields.size]
            val farm = fieldFarm.getValue(field)
            val product = listOf("GLYPHOSATE", "MAP", "UREA", "INSECTICIDE")[i % 4]
            val approved = i % 3 == 0
            PrescriptionEntity(
                id = DemoIds.uuid("rx-%03d".format(i)),
                farmId = DemoIds.uuid(farm),
                fieldId = DemoIds.uuid(field),
                product = product,
                rate = BigDecimal("${1 + i % 5}.${i % 10}"),
                unit = if (i % 2 == 0) "L/ha" else "kg/ha",
                status = if (approved) PrescriptionStatus.APPROVED.name else PrescriptionStatus.DRAFT.name,
                createdAt = now.minus(i.toLong(), ChronoUnit.DAYS),
                approvedAt = if (approved) now.minus((i - 1).toLong(), ChronoUnit.DAYS) else null,
                mode = PrescriptionMode.BROADCAST.name,
                treatedFraction = BigDecimal.ONE,
                activeIngredient = product,
                moaGroup = null,
                receituarioNumber = null,
                responsibleTechCpf = null,
                phiDays = null,
                reentryHours = null,
                fieldAreaHa = DemoFieldAreas.BY_KEY[field],
            )
        }
        // DEMO_CPF — not a real person (synthetic demo only)
        val demoCpf = "00000000191"
        val extra = listOf(
            PrescriptionEntity(
                id = DemoIds.uuid("rx-spot-001"),
                farmId = DemoIds.uuid("farm-001"),
                fieldId = DemoIds.uuid("field-001"),
                product = "GLYPHOSATE",
                rate = BigDecimal("2.5"),
                unit = "L/ha",
                status = PrescriptionStatus.APPROVED.name,
                createdAt = now.minus(3, ChronoUnit.DAYS),
                approvedAt = now.minus(2, ChronoUnit.DAYS),
                mode = PrescriptionMode.SPOT.name,
                treatedFraction = BigDecimal("0.35"),
                activeIngredient = "GLYPHOSATE",
                moaGroup = "G",
                receituarioNumber = "REC-DEMO-001",
                responsibleTechCpf = demoCpf,
                phiDays = 7,
                reentryHours = 24,
                fieldAreaHa = DemoFieldAreas.BY_KEY["field-001"],
            ),
            PrescriptionEntity(
                id = DemoIds.uuid("rx-moa-001"),
                farmId = DemoIds.uuid("farm-001"),
                fieldId = DemoIds.uuid("field-001"),
                product = "GLYPHOSATE",
                rate = BigDecimal("2.0"),
                unit = "L/ha",
                status = PrescriptionStatus.APPROVED.name,
                createdAt = now.minus(10, ChronoUnit.DAYS),
                approvedAt = now.minus(9, ChronoUnit.DAYS),
                mode = PrescriptionMode.BROADCAST.name,
                treatedFraction = BigDecimal.ONE,
                activeIngredient = "GLYPHOSATE",
                moaGroup = "G",
                receituarioNumber = "REC-DEMO-002",
                responsibleTechCpf = demoCpf,
                phiDays = 7,
                reentryHours = 24,
                fieldAreaHa = DemoFieldAreas.BY_KEY["field-001"],
            ),
            PrescriptionEntity(
                id = DemoIds.uuid("rx-draft-001"),
                farmId = DemoIds.uuid("farm-001"),
                fieldId = DemoIds.uuid("field-001"),
                product = "GLYPHOSATE",
                rate = BigDecimal("2.5"),
                unit = "L/ha",
                status = PrescriptionStatus.DRAFT.name,
                createdAt = now.minus(1, ChronoUnit.DAYS),
                approvedAt = null,
                mode = PrescriptionMode.BROADCAST.name,
                treatedFraction = BigDecimal.ONE,
                activeIngredient = "GLYPHOSATE",
                moaGroup = "G",
                receituarioNumber = null,
                responsibleTechCpf = demoCpf,
                phiDays = 7,
                reentryHours = 24,
                fieldAreaHa = DemoFieldAreas.BY_KEY["field-001"],
            ),
        )
        val all = baseRows + extra
        val existing = prescriptions.findAllById(all.map { it.id }).associateBy { it.id }
        val inserts = all.filter { it.id !in existing.keys }
        val backfills = existing.values.filter { needsBackfill(it) }.onEach { e ->
            if (e.activeIngredient.isNullOrBlank()) e.activeIngredient = e.product
            if (e.fieldAreaHa == null) e.fieldAreaHa = DemoFieldAreas.forFieldId(e.fieldId)
        }
        prescriptions.saveAll(inserts + backfills)
    }

    private fun needsBackfill(e: PrescriptionEntity): Boolean =
        e.activeIngredient.isNullOrBlank() || e.fieldAreaHa == null

    private fun ScoutingEntity.toDto() = ScoutingDto(id, farmId, fieldId, observedAt, pest, severity, notes, status)
    private fun SoilSampleEntity.toDto() = SoilSampleDto(id, farmId, fieldId, sampledAt, ph, organicMatterPct, pPpm, kPpm, labRef, status)
    private fun RecommendationEntity.toDto() = RecommendationDto(id, farmId, fieldId, kind, title, summary, priority, status, createdAt)
    private fun PrescriptionEntity.toDto() = PrescriptionDto(
        id = id,
        farmId = farmId,
        fieldId = fieldId,
        product = product,
        mode = mode,
        treatedFraction = treatedFraction,
        plannedDose = rate,
        unit = unit,
        activeIngredient = activeIngredient ?: product,
        moaGroup = moaGroup,
        receituarioNumber = receituarioNumber,
        responsibleTechCpf = responsibleTechCpf,
        phiDays = phiDays,
        reentryHours = reentryHours,
        fieldAreaHa = fieldAreaHa,
        status = status,
        createdAt = createdAt,
        approvedAt = approvedAt,
    )
}

@Service
class AgronomySeed(private val svc: AgronomyService, private val gate: com.precisionfarming.security.DemoSeedGate) {
    @Bean fun seedAgronomy() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
