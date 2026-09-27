package com.precisionfarming.mobile.data

/**
 * Picks the prescription to show for an operation.
 * - Prefer exact [prescriptionId] match when present.
 * - Else latest APPROVED with the same [fieldId].
 * - Else null (UI shows an em dash — never fabricate mode/MoA/fraction).
 */
fun selectPrescription(
    prescriptionId: String?,
    fieldId: String?,
    prescriptions: List<PrescriptionDto>,
): PrescriptionDto? {
    if (!prescriptionId.isNullOrBlank()) {
        return prescriptions.firstOrNull { it.id == prescriptionId }
    }
    val field = fieldId?.takeIf { it.isNotBlank() } ?: return null
    return prescriptions
        .filter { it.fieldId == field && it.status.equals("APPROVED", ignoreCase = true) }
        .maxByOrNull { it.approvedAt ?: it.createdAt.orEmpty() }
}
