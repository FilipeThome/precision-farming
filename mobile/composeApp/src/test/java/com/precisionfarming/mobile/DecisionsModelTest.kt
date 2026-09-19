package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.DecisionSources
import com.precisionfarming.mobile.data.DecisionStatus
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.InsightDto
import com.precisionfarming.mobile.data.IrrigationRecommendationDto
import com.precisionfarming.mobile.data.LOW_CONFIDENCE
import com.precisionfarming.mobile.data.PrescriptionDto
import com.precisionfarming.mobile.data.RecommendationDto
import com.precisionfarming.mobile.data.decisionId
import com.precisionfarming.mobile.data.needsHumanReview
import com.precisionfarming.mobile.data.normalizeStatus
import com.precisionfarming.mobile.data.parseDecisionId
import com.precisionfarming.mobile.data.toDecisionItems
import com.precisionfarming.mobile.data.DecisionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionsModelTest {
    private val field = FieldDto(
        id = "field-1",
        farmId = "farm-1",
        name = "Talhão Norte",
        areaHa = 25.1,
        crop = "SOY",
    )

    private val prescription = PrescriptionDto(
        id = "p1",
        farmId = "farm-1",
        fieldId = "field-1",
        product = "GLYPHOSATE",
        rate = 2.5,
        unit = "L/ha",
        status = "DRAFT",
        createdAt = "2026-09-10T08:00:00Z",
        approvedAt = null,
    )

    private val irrigation = IrrigationRecommendationDto(
        id = "i1",
        fieldId = "field-1",
        recommendedMm = 18.0,
        status = "PENDING",
        windowStart = "2026-09-10T08:05:00Z",
        reason = "WATER_DEFICIT",
    )

    private val insight = InsightDto(
        id = "a1",
        type = "YIELD_FORECAST",
        entityId = "field-1",
        score = 0.7,
        confidence = 0.41,
        horizonHours = 72,
        model = "yield-v1",
        modelVersion = "1.4",
        generatedAt = "2026-09-10T08:02:00Z",
        explanation = listOf("STABLE_NDVI", ""),
        demo = false,
    )

    @Test
    fun normalizeStatusAliases() {
        assertEquals(DecisionStatus.PENDING, normalizeStatus("DRAFT"))
        assertEquals(DecisionStatus.PENDING, normalizeStatus("recommended"))
        assertEquals(DecisionStatus.APPROVED, normalizeStatus("APPROVED"))
        assertEquals(DecisionStatus.REJECTED, normalizeStatus("REJECTED"))
        assertEquals(DecisionStatus.EXECUTED, normalizeStatus("APPLIED"))
        assertEquals(DecisionStatus.UNKNOWN, normalizeStatus(null))
        assertEquals(DecisionStatus.UNKNOWN, normalizeStatus("WHATEVER"))
        assertEquals(DecisionStatus.PENDING, normalizeStatus("  draft "))
        assertEquals(DecisionStatus.PENDING, normalizeStatus("open"))
        assertEquals(DecisionStatus.APPROVED, normalizeStatus("accepted"))
        assertEquals(DecisionStatus.REJECTED, normalizeStatus("canceled"))
        assertEquals(DecisionStatus.EXECUTED, normalizeStatus("completed"))
        assertEquals(DecisionStatus.UNKNOWN, normalizeStatus(""))
        assertEquals(DecisionStatus.UNKNOWN, normalizeStatus("   "))
    }

    @Test
    fun agronomyMinimalIsUnknown() {
        val item = toDecisionItems(DecisionSources(agronomy = listOf(RecommendationDto(id = "r1")))).single()
        assertEquals("AGRONOMY:r1", item.id)
        assertEquals(DecisionSource.AGRONOMY, item.source)
        assertEquals("RECOMMENDATION", item.title)
        assertEquals(DecisionStatus.UNKNOWN, item.status)
        assertFalse(item.capabilities.approve)
        assertNull(item.fieldId)
    }

    @Test
    fun agronomyTitleFallsBackToKind() {
        val byKind = toDecisionItems(DecisionSources(agronomy = listOf(RecommendationDto(id = "r1", kind = "FERTILIZATION")))).single()
        assertEquals("FERTILIZATION", byKind.title)
        val byTitle = toDecisionItems(
            DecisionSources(agronomy = listOf(RecommendationDto(id = "r1", kind = "FERTILIZATION", title = "Apply N"))),
        ).single()
        assertEquals("Apply N", byTitle.title)
    }

    @Test
    fun emptyStringsTreatedAsMissing() {
        val p = toDecisionItems(
            DecisionSources(
                prescriptions = listOf(
                    prescription.copy(fieldId = "", farmId = "", status = "", createdAt = "", approvedAt = ""),
                ),
            ),
        ).single()
        assertNull(p.fieldId)
        assertNull(p.farmId)
        assertNull(p.rawStatus)
        assertEquals(DecisionStatus.UNKNOWN, p.status)
        assertFalse(p.capabilities.approve)
    }

    @Test
    fun prescriptionApproveWhilePending() {
        val item = toDecisionItems(DecisionSources(prescriptions = listOf(prescription), fields = listOf(field))).single()
        assertEquals("PRESCRIPTION:p1", item.id)
        assertEquals(DecisionStatus.PENDING, item.status)
        assertEquals("DRAFT", item.rawStatus)
        assertEquals(2.5, item.quantity!!.value, 0.0)
        assertEquals("L/ha", item.quantity!!.unit)
        assertTrue(item.capabilities.approve)
    }

    @Test
    fun irrigationUsesRecommendedMm() {
        val item = toDecisionItems(DecisionSources(irrigation = listOf(irrigation), fields = listOf(field))).single()
        assertEquals(DecisionSource.IRRIGATION, item.source)
        assertEquals("farm-1", item.farmId)
        assertEquals(18.0, item.quantity!!.value, 0.0)
        assertEquals("mm", item.quantity!!.unit)
        assertTrue(item.capabilities.simulate)
    }

    @Test
    fun insightResolvesFieldAndNeedsReview() {
        val resolved = toDecisionItems(DecisionSources(insights = listOf(insight), fields = listOf(field))).single()
        assertEquals("field-1", resolved.fieldId)
        assertEquals(0.41, resolved.confidence!!, 0.0)
        assertEquals(listOf("STABLE_NDVI"), resolved.explanation)
        assertTrue(needsHumanReview(resolved))
        val unresolved = toDecisionItems(
            DecisionSources(insights = listOf(insight.copy(entityId = "machine-9")), fields = listOf(field)),
        ).single()
        assertNull(unresolved.fieldId)
        assertEquals("machine-9", unresolved.entityId)
    }

    @Test
    fun needsHumanReviewThreshold() {
        val base = toDecisionItems(DecisionSources(insights = listOf(insight))).single()
        assertFalse(needsHumanReview(base.copy(confidence = LOW_CONFIDENCE)))
        assertTrue(needsHumanReview(base.copy(confidence = LOW_CONFIDENCE - 0.01)))
        assertFalse(needsHumanReview(base.copy(confidence = null)))
    }

    @Test
    fun parseDecisionIdRoundTrip() {
        assertEquals(DecisionSource.PRESCRIPTION to "p1", parseDecisionId("PRESCRIPTION:p1"))
        assertNull(parseDecisionId("bogus"))
        assertNull(parseDecisionId("NOPE:x"))
        val id = decisionId(DecisionSource.AI_INSIGHT, "urn:a:b")
        assertEquals("AI_INSIGHT:urn:a:b", id)
        assertEquals(DecisionSource.AI_INSIGHT to "urn:a:b", parseDecisionId(id))
    }

    @Test
    fun emptySourcesYieldEmpty() {
        assertTrue(toDecisionItems(DecisionSources()).isEmpty())
    }

    @Test
    fun sourceOrderStable() {
        val items = toDecisionItems(
            DecisionSources(
                prescriptions = listOf(prescription.copy(id = "same")),
                irrigation = listOf(IrrigationRecommendationDto(id = "same")),
                agronomy = listOf(RecommendationDto(id = "same")),
                insights = listOf(insight.copy(id = "same")),
            ),
        )
        assertEquals(
            listOf(DecisionSource.PRESCRIPTION, DecisionSource.IRRIGATION, DecisionSource.AGRONOMY, DecisionSource.AI_INSIGHT),
            items.map { it.source },
        )
        assertEquals(4, items.map { it.id }.toSet().size)
    }
}
