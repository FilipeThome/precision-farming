package com.precisionfarming.agronomy

import com.precisionfarming.agronomy.domain.PrescriptionMode
import com.precisionfarming.agronomy.domain.PrescriptionStatus
import com.precisionfarming.agronomy.domain.moaRotationWarning
import com.precisionfarming.agronomy.domain.prescriptionIsoxml
import com.precisionfarming.agronomy.domain.requireDraftForTransition
import com.precisionfarming.agronomy.domain.requirePlannedDose
import com.precisionfarming.agronomy.domain.resolveTreatedFraction
import com.precisionfarming.agronomy.domain.spraySavings
import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class AgronomyDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("agronomy-001"), DemoIds.uuid("agronomy-001"))
    }

    @Test
    fun spraySavingsMath() {
        val s = spraySavings(BigDecimal("100"), BigDecimal("0.35"), BigDecimal("2.5"))
        assertEquals(0, BigDecimal("100").compareTo(s.fullRateHa))
        assertEquals(0, BigDecimal("35.0000").compareTo(s.treatedHa))
        assertEquals(0, BigDecimal("250.0000").compareTo(s.litersFullRate))
        assertEquals(0, BigDecimal("87.5000").compareTo(s.litersSpot))
        assertEquals(0, BigDecimal("162.5000").compareTo(s.litersAvoided))
        assertEquals(0, BigDecimal("2.5").compareTo(s.litersPerHa))
    }

    @Test
    fun moaWarningWhenTwoShareGroup() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val result = moaRotationWarning(listOf(a to "G", b to "G", c to "A"))
        assertTrue(result.warning)
        assertEquals("G", result.moaGroup)
        assertEquals(setOf(a, b), result.prescriptionIds.toSet())
    }

    @Test
    fun moaNoWarningForSingleGroup() {
        val result = moaRotationWarning(listOf(UUID.randomUUID() to "G", UUID.randomUUID() to "A"))
        assertFalse(result.warning)
        assertTrue(result.prescriptionIds.isEmpty())
    }

    @Test
    fun approveRejectOnlyFromDraft() {
        requireDraftForTransition(PrescriptionStatus.DRAFT.name)
        val ex = assertThrows(ConflictException::class.java) {
            requireDraftForTransition(PrescriptionStatus.APPROVED.name)
        }
        assertEquals("PRESCRIPTION_STATE_CONFLICT", ex.code)
    }

    @Test
    fun broadcastOmitsFractionAsOne() {
        assertEquals(0, BigDecimal.ONE.compareTo(resolveTreatedFraction(PrescriptionMode.BROADCAST, null)))
    }

    @Test
    fun broadcastRejectsFractionNotOne() {
        val ex = assertThrows(DomainException::class.java) {
            resolveTreatedFraction(PrescriptionMode.BROADCAST, BigDecimal("0.5"))
        }
        assertEquals("PRESCRIPTION_MODE_INVALID", ex.code)
    }

    @Test
    fun spotRequiresFractionBelowOne() {
        assertEquals(0, BigDecimal("0.35").compareTo(resolveTreatedFraction(PrescriptionMode.SPOT, BigDecimal("0.35"))))
        val ex = assertThrows(DomainException::class.java) {
            resolveTreatedFraction(PrescriptionMode.SPOT, BigDecimal.ONE)
        }
        assertEquals("PRESCRIPTION_MODE_INVALID", ex.code)
    }

    @Test
    fun fractionOutOfRange() {
        val ex = assertThrows(DomainException::class.java) {
            resolveTreatedFraction(PrescriptionMode.SPOT, BigDecimal("1.5"))
        }
        assertEquals("PRESCRIPTION_FRACTION_INVALID", ex.code)
    }

    @Test
    fun doseMustBePositive() {
        val ex = assertThrows(DomainException::class.java) { requirePlannedDose(BigDecimal.ZERO) }
        assertEquals("PRESCRIPTION_DOSE_INVALID", ex.code)
    }

    @Test
    fun isoxmlIncludesProductDoseUnitFractionAndEscapes() {
        val id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        val xml = prescriptionIsoxml(
            prescriptionId = id,
            product = "Herb & Co <SPOT>",
            plannedDose = BigDecimal("2.5"),
            unit = "L/ha",
            treatedFraction = BigDecimal("0.35"),
        )
        assertTrue(xml.contains("<ISO11783_TaskData"))
        assertTrue(xml.contains("<TSK "))
        assertTrue(xml.contains("<PDT "))
        assertTrue(xml.contains("<Product>Herb &amp; Co &lt;SPOT&gt;</Product>"))
        assertTrue(xml.contains("<PlannedDose>2.5</PlannedDose>"))
        assertTrue(xml.contains("<Unit>L/ha</Unit>"))
        assertTrue(xml.contains("<TreatedFraction>0.35</TreatedFraction>"))
        assertTrue(xml.contains(id.toString()))
    }
}
