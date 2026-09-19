package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.compactTodayTimeline
import com.precisionfarming.mobile.data.formatDecimal
import com.precisionfarming.mobile.data.formatTons
import com.precisionfarming.mobile.data.parseEpochMillis
import com.precisionfarming.mobile.data.todayRange
import com.precisionfarming.mobile.data.timelineRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class TodayTimelineTest {
    @Test
    fun todayRangeIsUtcMidnightSpan() {
        val now = Instant.parse("2026-09-18T15:30:00Z")
        val range = todayRange(now, ZoneOffset.UTC)
        assertEquals(Instant.parse("2026-09-18T00:00:00Z").toEpochMilli(), range.start)
        assertEquals(Instant.parse("2026-09-19T00:00:00Z").toEpochMilli(), range.end)
    }

    @Test
    fun includesOpsIntersectingDayWithOffsetTimestamps() {
        val now = Instant.parse("2026-09-18T12:00:00Z")
        val range = todayRange(now, ZoneOffset.UTC)
        val ops = listOf(
            OperationDto(
                id = "1",
                type = "SPRAYING",
                status = "PLANNED",
                plannedStart = "2026-09-18T08:00:00+00:00",
                plannedEnd = "2026-09-18T10:00:00+00:00",
            ),
            OperationDto(
                id = "2",
                type = "PLANTING",
                status = "PLANNED",
                plannedStart = "2026-09-17T08:00:00Z",
                plannedEnd = "2026-09-17T10:00:00Z",
            ),
        )
        val rows = timelineRows(ops, range, now.toEpochMilli())
        assertEquals(listOf("1"), rows.map { it.operation.id })
        assertNotNull(rows[0].planned)
    }

    @Test
    fun openEndedActualUsesNow() {
        val now = Instant.parse("2026-09-18T12:00:00Z")
        val range = todayRange(now, ZoneOffset.UTC)
        val rows = timelineRows(
            listOf(
                OperationDto(
                    id = "1",
                    type = "SPRAYING",
                    status = "IN_PROGRESS",
                    plannedStart = "2026-09-18T08:00:00Z",
                    plannedEnd = "2026-09-18T18:00:00Z",
                    actualStart = "2026-09-18T09:00:00Z",
                    actualEnd = null,
                ),
            ),
            range,
            now.toEpochMilli(),
        )
        assertNotNull(rows[0].executed)
        assertTrue(rows[0].executed!!.widthPct > 0)
    }

    @Test
    fun compactTodayTimelineDelegates() {
        val now = Instant.parse("2026-09-18T12:00:00Z")
        val rows = compactTodayTimeline(
            listOf(
                OperationDto(
                    id = "1",
                    type = "SPRAYING",
                    status = "PLANNED",
                    plannedStart = "2026-09-18T08:00:00Z",
                    plannedEnd = "2026-09-18T10:00:00Z",
                ),
            ),
            now = now,
            zone = ZoneOffset.UTC,
        )
        assertEquals(1, rows.size)
    }
}

class TimeParseTest {
    @Test
    fun parsesInstantAndOffset() {
        assertNotNull(parseEpochMillis("2026-09-18T12:00:00Z"))
        assertNotNull(parseEpochMillis("2026-09-18T12:00:00+00:00"))
        assertNull(parseEpochMillis(null))
        assertNull(parseEpochMillis(""))
        assertNull(parseEpochMillis("not-a-date"))
    }

    @Test
    fun formatHelpersAreLocaleStable() {
        assertEquals("1.50", formatDecimal(1.5))
        assertEquals("12.3", formatTons(12.34))
        assertEquals("12", formatTons(12.0))
    }
}
