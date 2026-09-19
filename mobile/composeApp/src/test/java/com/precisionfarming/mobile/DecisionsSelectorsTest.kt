package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.DecisionCapabilities
import com.precisionfarming.mobile.data.DecisionFilter
import com.precisionfarming.mobile.data.DecisionItem
import com.precisionfarming.mobile.data.DecisionSource
import com.precisionfarming.mobile.data.DecisionStatus
import com.precisionfarming.mobile.data.actionableDecisions
import com.precisionfarming.mobile.data.filterDecisions
import com.precisionfarming.mobile.data.pendingDecisions
import com.precisionfarming.mobile.data.sortByPriority
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionsSelectorsTest {
    private fun item(id: String, over: DecisionItem.() -> DecisionItem = { this }): DecisionItem =
        DecisionItem(
            id = id,
            rawId = id,
            source = DecisionSource.PRESCRIPTION,
            title = "X",
            status = DecisionStatus.UNKNOWN,
            capabilities = DecisionCapabilities(approve = false, simulate = false),
        ).over()

    private val pendingLow = item("a") { copy(status = DecisionStatus.PENDING, priority = "LOW", createdAt = "2026-09-01T00:00:00Z") }
    private val pendingHigh = item("b") { copy(status = DecisionStatus.PENDING, priority = "HIGH", createdAt = "2026-09-02T00:00:00Z") }
    private val approved = item("c") { copy(status = DecisionStatus.APPROVED, createdAt = "2026-09-03T00:00:00Z") }
    private val lowConf = item("d") {
        copy(source = DecisionSource.AI_INSIGHT, confidence = 0.4, createdAt = "2026-09-04T00:00:00Z")
    }
    private val highConf = item("e") {
        copy(source = DecisionSource.AI_INSIGHT, confidence = 0.9, createdAt = "2026-09-05T00:00:00Z")
    }
    private val newestPending = item("f") { copy(status = DecisionStatus.PENDING, createdAt = "2026-09-06T00:00:00Z") }

    @Test
    fun pendingKeepsOnlyPending() {
        assertEquals(listOf("a", "b"), pendingDecisions(listOf(pendingLow, approved, lowConf, pendingHigh)).map { it.id })
    }

    @Test
    fun actionableAddsLowConfidence() {
        assertEquals(listOf("d", "a"), actionableDecisions(listOf(approved, lowConf, highConf, pendingLow)).map { it.id })
    }

    @Test
    fun sortByPriorityOrder() {
        val sorted = sortByPriority(listOf(approved, pendingLow, lowConf, highConf, pendingHigh, newestPending))
        assertEquals(listOf("b", "a", "f", "d", "e", "c"), sorted.map { it.id })
    }

    @Test
    fun filterPendingApprovedAll() {
        val all = listOf(pendingLow, approved, lowConf)
        assertEquals(1, filterDecisions(all, DecisionFilter.PENDING).size)
        assertEquals(1, filterDecisions(all, DecisionFilter.APPROVED).size)
        assertEquals(3, filterDecisions(all, DecisionFilter.ALL).size)
    }

    @Test
    fun approvedIncludesExecutedExcludesRejected() {
        val executed = item("x") { copy(status = DecisionStatus.EXECUTED) }
        val rejected = item("r") { copy(status = DecisionStatus.REJECTED) }
        assertEquals(
            listOf("c", "x"),
            filterDecisions(listOf(approved, executed, rejected, pendingLow), DecisionFilter.APPROVED).map { it.id },
        )
    }

    @Test
    fun sortDoesNotMutateInput() {
        val undated = item("u") { copy(status = DecisionStatus.PENDING) }
        val dated = item("d") { copy(status = DecisionStatus.PENDING, createdAt = "2026-09-01T00:00:00Z") }
        val input = listOf(undated, dated)
        val sorted = sortByPriority(input)
        assertEquals(listOf("d", "u"), sorted.map { it.id })
        assertEquals(listOf("u", "d"), input.map { it.id })
    }
}
