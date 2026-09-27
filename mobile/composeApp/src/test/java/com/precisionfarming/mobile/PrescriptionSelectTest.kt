package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.PrescriptionDto
import com.precisionfarming.mobile.data.selectPrescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrescriptionSelectTest {
    private val fieldA = "field-a"
    private val fieldB = "field-b"

    private fun rx(
        id: String,
        fieldId: String,
        status: String,
        approvedAt: String? = null,
        createdAt: String? = null,
    ) = PrescriptionDto(
        id = id,
        farmId = "farm-1",
        fieldId = fieldId,
        product = "P",
        plannedDose = 1.0,
        unit = "L/ha",
        status = status,
        mode = "SPOT",
        treatedFraction = 0.3,
        moaGroup = "G9",
        createdAt = createdAt,
        approvedAt = approvedAt,
    )

    @Test
    fun prefersPrescriptionIdMatch() {
        val list = listOf(
            rx("p-old", fieldA, "APPROVED", approvedAt = "2026-09-20T00:00:00Z"),
            rx("p-match", fieldA, "DRAFT", approvedAt = null),
        )
        assertEquals("p-match", selectPrescription("p-match", fieldA, list)?.id)
    }

    @Test
    fun fallsBackToLatestApprovedForField() {
        val list = listOf(
            rx("p1", fieldA, "APPROVED", approvedAt = "2026-09-01T00:00:00Z"),
            rx("p2", fieldA, "APPROVED", approvedAt = "2026-09-10T00:00:00Z"),
            rx("p3", fieldB, "APPROVED", approvedAt = "2026-09-20T00:00:00Z"),
            rx("p4", fieldA, "DRAFT", approvedAt = null),
        )
        assertEquals("p2", selectPrescription(null, fieldA, list)?.id)
    }

    @Test
    fun missingReturnsNull() {
        assertNull(selectPrescription("missing", fieldA, listOf(rx("p1", fieldA, "APPROVED"))))
        assertNull(selectPrescription(null, fieldA, listOf(rx("p1", fieldA, "DRAFT"))))
        assertNull(selectPrescription(null, null, emptyList()))
        assertNull(selectPrescription(null, fieldA, emptyList()))
    }
}
