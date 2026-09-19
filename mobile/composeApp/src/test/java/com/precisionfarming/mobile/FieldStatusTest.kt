package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.FieldState
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.countByState
import com.precisionfarming.mobile.data.fieldStates
import org.junit.Assert.assertEquals
import org.junit.Test

class FieldStatusTest {
    private val fields = listOf(
        FieldDto(id = "a"),
        FieldDto(id = "b"),
        FieldDto(id = "c"),
    )

    private fun op(
        fieldId: String,
        status: String,
        actualEnd: String? = null,
        plannedEnd: String? = null,
    ) = OperationDto(
        id = "$fieldId-$status",
        fieldId = fieldId,
        type = "SPRAYING",
        status = status,
        actualEnd = actualEnd,
        plannedEnd = plannedEnd,
    )

    @Test
    fun inProgressWins() {
        assertEquals(
            FieldState.progress,
            fieldStates(fields, listOf(op("a", "COMPLETED"), op("a", "PLANNED"), op("a", "IN_PROGRESS")))["a"],
        )
    }

    @Test
    fun pausedMapsToBlocked() {
        assertEquals(FieldState.blocked, fieldStates(fields, listOf(op("a", "PAUSED"), op("a", "PLANNED")))["a"])
    }

    @Test
    fun fieldsWithoutOpsAreNone() {
        val states = fieldStates(fields, listOf(op("a", "PLANNED"), op("zzz", "IN_PROGRESS")))
        assertEquals(FieldState.planned, states["a"])
        assertEquals(FieldState.none, states["b"])
        assertEquals(FieldState.none, states["c"])
        assertEquals(1, countByState(states)[FieldState.planned])
        assertEquals(2, countByState(states)[FieldState.none])
    }

    @Test
    fun ignoresUnknownStatusesAsNone() {
        assertEquals(FieldState.none, fieldStates(fields, listOf(op("a", "CANCELLED")))["a"])
        assertEquals(FieldState.done, fieldStates(fields, listOf(op("a", "CANCELLED"), op("a", "COMPLETED")))["a"])
        assertEquals(FieldState.progress, fieldStates(fields, listOf(op("a", "in_progress")))["a"])
    }

    @Test
    fun completedOlderThanThresholdBecomesStale() {
        val now = 1_700_000_000_000L
        val old = "2023-01-01T00:00:00Z"
        val states = fieldStates(
            listOf(FieldDto(id = "a")),
            listOf(op("a", "COMPLETED", actualEnd = old)),
            now = now,
            staleAfterMs = 24L * 60 * 60 * 1000,
        )
        assertEquals(FieldState.stale, states["a"])
    }

    @Test
    fun recentCompletedStaysDone() {
        val now = 1_700_000_000_000L
        val recent = java.time.Instant.ofEpochMilli(now - 3_600_000).toString()
        val states = fieldStates(
            listOf(FieldDto(id = "a")),
            listOf(op("a", "COMPLETED", actualEnd = recent)),
            now = now,
        )
        assertEquals(FieldState.done, states["a"])
    }

    @Test
    fun countByStateZeroed() {
        assertEquals(0, countByState(emptyMap())[FieldState.progress])
        val counts = countByState(mapOf("a" to FieldState.done, "b" to FieldState.done, "c" to FieldState.blocked))
        assertEquals(2, counts[FieldState.done])
        assertEquals(1, counts[FieldState.blocked])
    }
}
