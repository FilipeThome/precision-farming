package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.HarvestPlanDto
import com.precisionfarming.mobile.data.InspectNav
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.YieldMath
import com.precisionfarming.mobile.data.YieldRecordDto
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.data.canComplete
import com.precisionfarming.mobile.data.canPause
import com.precisionfarming.mobile.data.canStart
import com.precisionfarming.mobile.data.isOpen
import com.precisionfarming.mobile.data.offline.OpCommandType
import com.precisionfarming.mobile.data.offline.QueueState
import com.precisionfarming.mobile.data.offline.QueuedCommand
import com.precisionfarming.mobile.data.offline.SyncState
import com.precisionfarming.mobile.data.withQueuedStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YieldMathTest {
    @Test
    fun emptyIsZero() {
        assertEquals(0.0, YieldMath.weightedYieldTHa(emptyList()), 0.0)
    }

    @Test
    fun averagesWhenAreaMissing() {
        val rows = listOf(
            YieldRecordDto(id = "a", yieldTHa = 4.0),
            YieldRecordDto(id = "b", yieldTHa = 6.0),
        )
        assertEquals(5.0, YieldMath.weightedYieldTHa(rows), 0.0)
    }

    @Test
    fun weightsByAreaWhenPresent() {
        val rows = listOf(
            YieldRecordDto(id = "a", yieldTHa = 2.0, areaHa = 10.0),
            YieldRecordDto(id = "b", yieldTHa = 4.0, areaHa = 30.0),
        )
        assertEquals(3.5, YieldMath.weightedYieldTHa(rows), 0.0)
    }

    @Test
    fun skipsNullAndNonFiniteYields() {
        val rows = listOf(
            YieldRecordDto(id = "a", yieldTHa = null),
            YieldRecordDto(id = "b", yieldTHa = Double.NaN),
            YieldRecordDto(id = "c", yieldTHa = Double.POSITIVE_INFINITY),
            YieldRecordDto(id = "d", yieldTHa = 8.0),
        )
        assertEquals(8.0, YieldMath.weightedYieldTHa(rows), 0.0)
    }

    @Test
    fun mixedAreaUsesOnlyRowsWithPositiveArea() {
        val rows = listOf(
            YieldRecordDto(id = "a", yieldTHa = 10.0),
            YieldRecordDto(id = "b", yieldTHa = 2.0, areaHa = 10.0),
            YieldRecordDto(id = "c", yieldTHa = 4.0, areaHa = 30.0),
            YieldRecordDto(id = "d", yieldTHa = 99.0, areaHa = 0.0),
        )
        assertEquals(3.5, YieldMath.weightedYieldTHa(rows), 0.0)
    }

    @Test
    fun yieldsForPlanKeepsPlanAndUnscopedFieldRows() {
        val plan = HarvestPlanDto(id = "p1", fieldId = "f1")
        val rows = listOf(
            YieldRecordDto(id = "a", fieldId = "f1", planId = "p1", yieldTHa = 3.0),
            YieldRecordDto(id = "b", fieldId = "f1", planId = null, yieldTHa = 4.0),
            YieldRecordDto(id = "c", fieldId = "f1", planId = "p2", yieldTHa = 9.0),
            YieldRecordDto(id = "d", fieldId = "f2", planId = null, yieldTHa = 7.0),
        )
        assertEquals(listOf("a", "b"), YieldMath.yieldsForPlan(rows, plan).map { it.id })
    }

    @Test
    fun yieldsForFieldKeepsMatchingFieldOnly() {
        val rows = listOf(
            YieldRecordDto(id = "a", fieldId = "f1", yieldTHa = 3.0),
            YieldRecordDto(id = "b", fieldId = "f2", yieldTHa = 4.0),
            YieldRecordDto(id = "c", fieldId = "f1", planId = "p2", yieldTHa = 9.0),
            YieldRecordDto(id = "d", fieldId = null, yieldTHa = 1.0),
        )
        assertEquals(listOf("a", "c"), YieldMath.yieldsForField(rows, "f1").map { it.id })
        assertEquals(emptyList<String>(), YieldMath.yieldsForField(rows, "missing").map { it.id })
    }
}

class InspectNavTest {
    @Test
    fun hrefUsesQuerySelectedNotPathId() {
        assertEquals("ops?selected=abc", InspectNav.href(InspectNav.OPS, selected = "abc"))
        assertEquals(
            "alertas?severity=CRITICAL&selected=a1",
            InspectNav.href(InspectNav.ALERTS, selected = "a1", severity = "CRITICAL"),
        )
        assertTrue(InspectNav.tabSelected("ops?selected={selected}", InspectNav.OPS))
        assertTrue(InspectNav.tabSelected("mais/machines?selected={selected}", InspectNav.MORE))
        assertFalse(InspectNav.tabSelected("ops?selected={selected}", InspectNav.HOME))
    }

    @Test
    fun emptySelectedKeepsQueryKey() {
        assertEquals("ops?selected=", InspectNav.href(InspectNav.OPS))
        assertEquals("ops?selected=", InspectNav.href(InspectNav.OPS, selected = ""))
        assertEquals("alertas?severity=&selected=", InspectNav.href(InspectNav.ALERTS))
        assertEquals("ops?selected={selected}", InspectNav.pattern(InspectNav.OPS))
        assertEquals(
            "alertas?severity={severity}&selected={selected}",
            InspectNav.pattern(InspectNav.ALERTS),
        )
        assertEquals("ops", InspectNav.baseOf("ops?selected=abc"))
        assertEquals("", InspectNav.baseOf(null))
    }

    @Test
    fun byIdIgnoresBlankAndMisses() {
        val items = listOf(OperationDto("o1", "PLANT", "PLANNED"), OperationDto("o2", "SPRAY", "PAUSED"))
        assertEquals("o1", items.byId("o1") { it.id }?.id)
        assertNull(items.byId(null) { it.id })
        assertNull(items.byId("") { it.id })
        assertNull(items.byId("   ") { it.id })
        assertNull(items.byId("missing") { it.id })
    }
}

class OpGatesTest {
    @Test
    fun statusGatesMatchWeb() {
        assertTrue(OperationDto("1", "PLANT", "PLANNED").canStart())
        assertTrue(OperationDto("1", "PLANT", "PAUSED").canStart())
        assertFalse(OperationDto("1", "PLANT", "IN_PROGRESS").canStart())
        assertTrue(OperationDto("1", "PLANT", "IN_PROGRESS").canPause())
        assertFalse(OperationDto("1", "PLANT", "PLANNED").canPause())
        assertFalse(OperationDto("1", "PLANT", "PAUSED").canPause())
        assertTrue(OperationDto("1", "PLANT", "IN_PROGRESS").canComplete())
        assertTrue(OperationDto("1", "PLANT", "PAUSED").canComplete())
        assertFalse(OperationDto("1", "PLANT", "PLANNED").canComplete())
        assertFalse(OperationDto("1", "PLANT", "COMPLETED").canStart())
        assertFalse(OperationDto("1", "PLANT", "COMPLETED").canPause())
        assertFalse(OperationDto("1", "PLANT", "COMPLETED").canComplete())
    }

    @Test
    fun statusGatesAreCaseInsensitive() {
        assertTrue(OperationDto("1", "PLANT", "planned").canStart())
        assertTrue(OperationDto("1", "PLANT", "in_progress").canPause())
        assertTrue(OperationDto("1", "PLANT", "paused").canComplete())
    }

    @Test
    fun queuedSyncedStartHidesStartUntilReload() {
        val planned = OperationDto("1", "PLANT", "PLANNED")
        val queue = QueueState(
            items = listOf(
                QueuedCommand(
                    "c1",
                    "1",
                    OpCommandType.START,
                    createdAt = "2026-01-01T00:00:00Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:01Z",
                ),
            ),
        )
        val shown = planned.withQueuedStatus(queue)
        assertEquals("IN_PROGRESS", shown.status)
        assertFalse(shown.canStart())
        assertTrue(shown.canPause())
        assertTrue(shown.canComplete())
    }

    @Test
    fun serverCompletedOutranksLocalStart() {
        val completed = OperationDto("1", "PLANT", "COMPLETED")
        val queue = QueueState(
            items = listOf(
                QueuedCommand(
                    "c1",
                    "1",
                    OpCommandType.START,
                    createdAt = "2026-01-01T00:00:00Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:01Z",
                ),
            ),
        )
        val shown = completed.withQueuedStatus(queue)
        assertEquals("COMPLETED", shown.status)
        assertFalse(shown.canStart())
        assertFalse(shown.canPause())
        assertFalse(shown.canComplete())
    }

    @Test
    fun alertIsOpenMatchesOpenStatus() {
        assertTrue(AlertDto("a1", "Storm", severity = "CRITICAL", status = "OPEN").isOpen())
        assertTrue(AlertDto("a2", "Storm", severity = "WARNING", status = "open").isOpen())
        assertFalse(AlertDto("a3", "Old", severity = "CRITICAL", status = "ACKED").isOpen())
        assertFalse(AlertDto("a4", "Done", severity = "INFO", status = "CLOSED").isOpen())
    }
}

class FarmFilterTest {
    @Test
    fun applyIgnoresBlankAndSetsFarm() {
        FarmFilter.farmId = null
        FarmFilter.apply(null)
        FarmFilter.apply("")
        assertNull(FarmFilter.farmId)
        FarmFilter.apply("farm-9")
        assertEquals("farm-9", FarmFilter.farmId)
        FarmFilter.farmId = null
    }
}
