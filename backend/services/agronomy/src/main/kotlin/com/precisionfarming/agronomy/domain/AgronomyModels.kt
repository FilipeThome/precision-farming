package com.precisionfarming.agronomy.domain

enum class PrescriptionStatus { DRAFT, APPROVED, REJECTED }
enum class ObservationSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class LabResult(
    val labRef: String,
    val ph: java.math.BigDecimal,
    val organicMatterPct: java.math.BigDecimal,
    val pPpm: java.math.BigDecimal,
    val kPpm: java.math.BigDecimal,
)

interface LabAdapter {
    fun analyze(sampleKey: String): LabResult
}
