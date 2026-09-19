package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.CHAIN_STEP_IDS
import com.precisionfarming.mobile.data.ChainStepId
import com.precisionfarming.mobile.data.ChainStepState
import com.precisionfarming.mobile.data.DecisionCapabilities
import com.precisionfarming.mobile.data.DecisionItem
import com.precisionfarming.mobile.data.DecisionSource
import com.precisionfarming.mobile.data.DecisionStatus
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.chainSteps
import com.precisionfarming.mobile.data.linkedOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionChainTest {
    private val base = DecisionItem(
        id = "PRESCRIPTION:p1",
        rawId = "p1",
        source = DecisionSource.PRESCRIPTION,
        title = "GLYPHOSATE",
        fieldId = "field-1",
        farmId = "farm-1",
        status = DecisionStatus.PENDING,
        createdAt = "2026-09-10T08:00:00Z",
        capabilities = DecisionCapabilities(approve = true, simulate = false),
    )

    private val op = OperationDto(
        id = "op-1",
        fieldId = "field-1",
        farmId = "farm-1",
        type = "SPRAYING",
        status = "IN_PROGRESS",
        plannedStart = "2026-09-10T10:00:00Z",
        plannedEnd = "2026-09-10T14:00:00Z",
        actualStart = "2026-09-10T10:05:00Z",
    )

    private fun states(item: DecisionItem, ops: List<OperationDto> = emptyList()) =
        chainSteps(item, ops).associate { it.id to it.state }

    @Test
    fun pendingApprovalIsNow() {
        assertEquals(
            mapOf(
                ChainStepId.SIGNAL to ChainStepState.DONE,
                ChainStepId.CONTEXT to ChainStepState.DONE,
                ChainStepId.RECOMMENDATION to ChainStepState.DONE,
                ChainStepId.APPROVAL to ChainStepState.NOW,
                ChainStepId.ORDER to ChainStepState.PENDING,
                ChainStepId.EXECUTION to ChainStepState.PENDING,
            ),
            states(base),
        )
    }

    @Test
    fun approvedWithoutOpOrderIsNow() {
        assertEquals(ChainStepState.DONE, states(base.copy(status = DecisionStatus.APPROVED, approvedAt = "2026-09-10T09:00:00Z"))[ChainStepId.APPROVAL])
        assertEquals(ChainStepState.NOW, states(base.copy(status = DecisionStatus.APPROVED, approvedAt = "2026-09-10T09:00:00Z"))[ChainStepId.ORDER])
    }

    @Test
    fun approvedWithLinkedOpInProgress() {
        val steps = chainSteps(base.copy(status = DecisionStatus.APPROVED, approvedAt = "2026-09-10T09:00:00Z"), listOf(op))
        val byId = steps.associateBy { it.id }
        assertEquals(ChainStepState.DONE, byId.getValue(ChainStepId.ORDER).state)
        assertTrue(byId.getValue(ChainStepId.ORDER).inferred)
        assertEquals(ChainStepState.NOW, byId.getValue(ChainStepId.EXECUTION).state)
    }

    @Test
    fun rejectedBlocksApproval() {
        assertEquals(ChainStepState.BLOCKED, states(base.copy(status = DecisionStatus.REJECTED))[ChainStepId.APPROVAL])
    }

    @Test
    fun sixStepsInOrder() {
        assertEquals(CHAIN_STEP_IDS, chainSteps(base).map { it.id })
    }

    @Test
    fun linkedOperationRequiresApprovalAfterStart() {
        val approved = base.copy(status = DecisionStatus.APPROVED, approvedAt = "2026-09-10T12:00:00Z")
        assertNull(linkedOperation(approved, listOf(op)))
        assertEquals("op-1", linkedOperation(approved.copy(approvedAt = "2026-09-10T09:00:00Z"), listOf(op))?.id)
    }

    @Test
    fun ambiguousOpsNotLinked() {
        val approved = base.copy(status = DecisionStatus.APPROVED, approvedAt = "2026-09-10T09:00:00Z")
        val later = op.copy(id = "later", actualStart = "2026-09-10T12:00:00Z")
        val earlier = op.copy(id = "earlier", actualStart = "2026-09-10T09:30:00Z")
        assertNull(linkedOperation(approved, listOf(later, earlier)))
        assertEquals("earlier", linkedOperation(approved, listOf(earlier, op.copy(id = "before", actualStart = "2026-09-10T08:00:00Z")))?.id)
    }
}
