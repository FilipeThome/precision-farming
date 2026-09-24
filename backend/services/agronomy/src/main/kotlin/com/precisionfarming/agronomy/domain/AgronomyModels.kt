package com.precisionfarming.agronomy.domain

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DomainException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

enum class PrescriptionStatus { DRAFT, APPROVED, REJECTED }
enum class PrescriptionMode { BROADCAST, SPOT }
enum class ObservationSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class LabResult(
    val labRef: String,
    val ph: BigDecimal,
    val organicMatterPct: BigDecimal,
    val pPpm: BigDecimal,
    val kPpm: BigDecimal,
)

interface LabAdapter {
    fun analyze(sampleKey: String): LabResult
}

data class SpraySavings(
    val fieldAreaHa: BigDecimal,
    val treatedHa: BigDecimal,
    val fullRateHa: BigDecimal,
    val litersFullRate: BigDecimal,
    val litersSpot: BigDecimal,
    val litersAvoided: BigDecimal,
    val litersPerHa: BigDecimal,
)

data class MoaRotationResult(
    val moaGroup: String?,
    val warning: Boolean,
    val prescriptionIds: List<UUID>,
)

/** fullRateHa=fieldAreaHa; treatedHa=fieldAreaHa*treatedFraction; liters* from plannedDose. */
fun spraySavings(fieldAreaHa: BigDecimal, treatedFraction: BigDecimal, plannedDose: BigDecimal): SpraySavings {
    val fullRateHa = fieldAreaHa
    val treatedHa = fieldAreaHa.multiply(treatedFraction).setScale(4, RoundingMode.HALF_UP)
    val litersFullRate = plannedDose.multiply(fullRateHa).setScale(4, RoundingMode.HALF_UP)
    val litersSpot = plannedDose.multiply(treatedHa).setScale(4, RoundingMode.HALF_UP)
    val litersAvoided = litersFullRate.subtract(litersSpot).setScale(4, RoundingMode.HALF_UP)
    return SpraySavings(
        fieldAreaHa = fieldAreaHa,
        treatedHa = treatedHa,
        fullRateHa = fullRateHa,
        litersFullRate = litersFullRate,
        litersSpot = litersSpot,
        litersAvoided = litersAvoided,
        litersPerHa = plannedDose,
    )
}

/** warning when ≥2 prescriptions share the same non-blank moaGroup. */
fun moaRotationWarning(prescriptions: List<Pair<UUID, String?>>): MoaRotationResult {
    val byGroup = prescriptions
        .mapNotNull { (id, group) -> group?.takeIf { it.isNotBlank() }?.let { id to it } }
        .groupBy({ it.second }, { it.first })
    val conflict = byGroup.entries.firstOrNull { it.value.size >= 2 }
    return if (conflict == null) {
        MoaRotationResult(moaGroup = byGroup.keys.firstOrNull(), warning = false, prescriptionIds = emptyList())
    } else {
        MoaRotationResult(moaGroup = conflict.key, warning = true, prescriptionIds = conflict.value)
    }
}

fun resolveTreatedFraction(mode: PrescriptionMode, treatedFraction: BigDecimal?): BigDecimal {
    val fraction = when {
        treatedFraction == null && mode == PrescriptionMode.BROADCAST -> BigDecimal.ONE
        treatedFraction == null -> throw DomainException(
            "PRESCRIPTION_MODE_INVALID",
            "SPOT requires treatedFraction < 1",
        )
        else -> treatedFraction
    }
    if (fraction < BigDecimal.ZERO || fraction > BigDecimal.ONE) {
        throw DomainException("PRESCRIPTION_FRACTION_INVALID", "treatedFraction must be in [0,1]")
    }
    when (mode) {
        PrescriptionMode.BROADCAST -> {
            if (treatedFraction != null && fraction.compareTo(BigDecimal.ONE) != 0) {
                throw DomainException("PRESCRIPTION_MODE_INVALID", "BROADCAST requires treatedFraction = 1")
            }
        }
        PrescriptionMode.SPOT -> {
            if (fraction.compareTo(BigDecimal.ONE) >= 0) {
                throw DomainException("PRESCRIPTION_MODE_INVALID", "SPOT requires treatedFraction < 1")
            }
        }
    }
    return fraction
}

fun requirePlannedDose(plannedDose: BigDecimal) {
    if (plannedDose.signum() <= 0) {
        throw DomainException("PRESCRIPTION_DOSE_INVALID", "plannedDose must be > 0")
    }
}

fun requireDraftForTransition(status: String) {
    if (status != PrescriptionStatus.DRAFT.name) {
        throw ConflictException("PRESCRIPTION_STATE_CONFLICT", "Prescription must be DRAFT")
    }
}

/** Minimal ISO11783 TaskData export for machine controllers (product / dose / unit / treated fraction). */
fun prescriptionIsoxml(
    prescriptionId: UUID,
    product: String,
    plannedDose: BigDecimal,
    unit: String,
    treatedFraction: BigDecimal,
): String = buildString {
    append("""<?xml version="1.0" encoding="UTF-8"?>""")
    append("""<ISO11783_TaskData VersionMajor="4" VersionMinor="0">""")
    append("""<TSK A="TSK-""").append(escapeXml(prescriptionId.toString())).append("\">")
    append("""<PDT A="PDT1">""")
    append("<Product>").append(escapeXml(product)).append("</Product>")
    append("<PlannedDose>").append(escapeXml(plannedDose.toPlainString())).append("</PlannedDose>")
    append("<Unit>").append(escapeXml(unit)).append("</Unit>")
    append("<TreatedFraction>").append(escapeXml(treatedFraction.toPlainString())).append("</TreatedFraction>")
    append("</PDT>")
    append("</TSK>")
    append("</ISO11783_TaskData>")
}

internal fun escapeXml(value: String): String = buildString(value.length) {
    for (ch in value) {
        when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(ch)
        }
    }
}

/** Local ha map — mirrors OperationService.FIELD_AREA / farm seed; do not call farm-service. */
object DemoFieldAreas {
    val BY_KEY: Map<String, BigDecimal> = mapOf(
        "field-001" to BigDecimal("120.5"),
        "field-002" to BigDecimal("95.0"),
        "field-003" to BigDecimal("80.0"),
        "field-004" to BigDecimal("210.0"),
        "field-005" to BigDecimal("175.0"),
        "field-006" to BigDecimal("320.0"),
        "field-007" to BigDecimal("280.0"),
        "field-008" to BigDecimal("190.0"),
        "field-009" to BigDecimal("150.0"),
        "field-010" to BigDecimal("140.0"),
        "field-011" to BigDecimal("110.0"),
        "field-012" to BigDecimal("95.0"),
        "field-013" to BigDecimal("88.0"),
        "field-014" to BigDecimal("70.0"),
        "field-015" to BigDecimal("130.0"),
        "field-016" to BigDecimal("102.0"),
        "field-017" to BigDecimal("155.0"),
        "field-018" to BigDecimal("140.0"),
        "field-019" to BigDecimal("125.0"),
        "field-020" to BigDecimal("118.0"),
        "field-021" to BigDecimal("105.0"),
        "field-022" to BigDecimal("98.0"),
    )

    fun forFieldId(fieldId: UUID): BigDecimal? =
        BY_KEY.entries.firstOrNull { com.precisionfarming.common.DemoIds.uuid(it.key) == fieldId }?.value
}
