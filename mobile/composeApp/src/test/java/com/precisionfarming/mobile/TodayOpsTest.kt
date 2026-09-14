package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.TodayOps
import com.precisionfarming.mobile.data.WeatherWindowDto
import com.precisionfarming.mobile.data.offline.OpCommandType
import com.precisionfarming.mobile.data.offline.QueueState
import com.precisionfarming.mobile.data.offline.QueuedCommand
import com.precisionfarming.mobile.data.offline.SyncState
import com.precisionfarming.mobile.data.withQueuedStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TodayOpsTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    // 2026-03-10 09:00 local (-03:00)
    private val now = Instant.parse("2026-03-10T12:00:00Z")

    private fun op(
        id: String,
        status: String,
        plannedStart: String? = null,
        plannedEnd: String? = null,
        actualStart: String? = null,
    ) = OperationDto(id = id, type = "SPRAYING", status = status, plannedStart = plannedStart, plannedEnd = plannedEnd, actualStart = actualStart)

    @Test
    fun operationsTodayUsesLocalDayAndIntersection() {
        val ops = listOf(
            op("early", "PLANNED", "2026-03-10T06:00:00-03:00", "2026-03-10T10:30:00-03:00"),
            op("late", "PLANNED", "2026-03-10T23:00:00-03:00", "2026-03-11T02:00:00-03:00"),
            op("yesterday", "COMPLETED", "2026-03-09T06:00:00-03:00", "2026-03-09T10:00:00-03:00"),
            op("spansToday", "IN_PROGRESS", "2026-03-09T22:00:00-03:00", "2026-03-10T01:00:00-03:00"),
            op("startOnly", "PLANNED", "2026-03-10T14:00:00-03:00"),
            op("unscheduled", "PLANNED"),
            // 02:30Z = 23:30 local on the 9th → not today in São Paulo
            op("utcTrap", "PLANNED", "2026-03-10T02:30:00Z", "2026-03-10T02:45:00Z"),
        )
        val today = TodayOps.operationsToday(ops, now, zone).map { it.id }
        assertEquals(listOf("spansToday", "early", "startOnly", "late"), today)
    }

    @Test
    fun nextActionablePrefersRunningThenEarliestStartable() {
        val ops = listOf(
            op("planned-later", "PLANNED", "2026-03-11T06:00:00-03:00"),
            op("planned-soon", "PLANNED", "2026-03-10T14:00:00-03:00"),
            op("paused", "PAUSED", "2026-03-10T15:00:00-03:00"),
            op("done", "COMPLETED", "2026-03-10T05:00:00-03:00"),
        )
        assertEquals("planned-soon", TodayOps.nextActionable(ops, zone)?.id)
        val withRunning = ops + op("running", "IN_PROGRESS", "2026-03-12T06:00:00-03:00", actualStart = "2026-03-10T08:00:00-03:00")
        assertEquals("running", TodayOps.nextActionable(withRunning, zone)?.id)
        assertNull(TodayOps.nextActionable(listOf(op("done", "COMPLETED")), zone))
        assertNull(TodayOps.nextActionable(emptyList(), zone))
    }

    @Test
    fun nextActionablePutsUnscheduledLast() {
        val ops = listOf(op("unscheduled", "PLANNED"), op("scheduled", "PLANNED", "2026-03-10T14:00:00-03:00"))
        assertEquals("scheduled", TodayOps.nextActionable(ops, zone)?.id)
        assertEquals("unscheduled", TodayOps.nextActionable(listOf(op("unscheduled", "PLANNED")), zone)?.id)
    }

    @Test
    fun kpisCountTodayCompletedAndOpenAlerts() {
        val ops = listOf(
            op("a", "COMPLETED", "2026-03-10T06:00:00-03:00", "2026-03-10T08:00:00-03:00"),
            op("b", "PLANNED", "2026-03-10T14:00:00-03:00", "2026-03-10T16:00:00-03:00"),
            op("c", "COMPLETED", "2026-03-09T06:00:00-03:00", "2026-03-09T08:00:00-03:00"),
        )
        val alerts = listOf(
            AlertDto(id = "1", title = "x", severity = "CRITICAL", status = "OPEN"),
            AlertDto(id = "2", title = "y", severity = "INFO", status = "ACKED"),
        )
        val kpis = TodayOps.todayKpis(ops, alerts, now, zone)
        assertEquals(2, kpis.opsToday)
        assertEquals(1, kpis.completedToday)
        assertEquals(1, kpis.openAlerts)
        assertEquals("1/2", kpis.completedLabel)
        assertNull(TodayOps.todayKpis(emptyList(), alerts, now, zone).completedLabel)
    }

    @Test
    fun resolveZoneUsesFarmTimezoneOnlyWhenSelected() {
        val farms = listOf(FarmDto(id = "f1", name = "A", location = "x", timezone = "America/Manaus"), FarmDto(id = "f2", name = "B", location = "y"))
        val fallback = ZoneId.of("UTC")
        assertEquals(ZoneId.of("America/Manaus"), TodayOps.resolveZone(farms, "f1", fallback))
        assertEquals(fallback, TodayOps.resolveZone(farms, "f2", fallback))
        assertEquals(fallback, TodayOps.resolveZone(farms, null, fallback))
        assertEquals(fallback, TodayOps.resolveZone(listOf(farms[0].copy(timezone = "Not/AZone")), "f1", fallback))
    }

    @Test
    fun weatherRatingOnlyWhenWindowIntersects() {
        val op = op("a", "PLANNED", "2026-03-10T06:00:00-03:00", "2026-03-10T10:30:00-03:00")
        val windows = listOf(
            WeatherWindowDto(id = "w1", startAt = "2026-03-10T12:00:00-03:00", endAt = "2026-03-10T18:00:00-03:00", rating = "UNFAVORABLE"),
            WeatherWindowDto(id = "w2", startAt = "2026-03-10T05:00:00-03:00", endAt = "2026-03-10T09:00:00-03:00", rating = "FAVORABLE"),
        )
        assertEquals("FAVORABLE", TodayOps.weatherRatingFor(op, windows, zone))
        assertNull(TodayOps.weatherRatingFor(op, listOf(windows[0]), zone))
        assertNull(TodayOps.weatherRatingFor(op.copy(plannedStart = null), windows, zone))
    }

    @Test
    fun greetingBuckets() {
        val utc = ZoneId.of("UTC")
        assertEquals("today.greeting.morning", TodayOps.greetingKey(Instant.parse("2026-03-10T00:00:00Z"), utc))
        assertEquals("today.greeting.morning", TodayOps.greetingKey(Instant.parse("2026-03-10T11:59:59Z"), utc))
        assertEquals("today.greeting.afternoon", TodayOps.greetingKey(Instant.parse("2026-03-10T12:00:00Z"), utc))
        assertEquals("today.greeting.afternoon", TodayOps.greetingKey(Instant.parse("2026-03-10T17:59:59Z"), utc))
        assertEquals("today.greeting.evening", TodayOps.greetingKey(Instant.parse("2026-03-10T18:00:00Z"), utc))
        assertEquals("today.greeting.evening", TodayOps.greetingKey(Instant.parse("2026-03-10T23:59:59Z"), utc))
    }

    @Test
    fun localMidnightBoundaryFlipsTheCalendarDay() {
        val justBefore = Instant.parse("2026-03-10T02:59:59Z") // 23:59:59 on Mar 9 in São Paulo
        val atMidnight = Instant.parse("2026-03-10T03:00:00Z") // 00:00 local Mar 10
        val endsAtMidnight = op("endsAtMidnight", "PLANNED", "2026-03-09T20:00:00-03:00", "2026-03-10T00:00:00-03:00")
        val startsAtMidnight = op("startsAtMidnight", "PLANNED", "2026-03-10T00:00:00-03:00", "2026-03-10T02:00:00-03:00")
        val crossesMidnight = op("crosses", "IN_PROGRESS", "2026-03-09T22:00:00-03:00", "2026-03-10T01:00:00-03:00")

        assertEquals(listOf("endsAtMidnight", "crosses"), TodayOps.operationsToday(listOf(endsAtMidnight, startsAtMidnight, crossesMidnight), justBefore, zone).map { it.id })
        assertEquals(listOf("crosses", "startsAtMidnight"), TodayOps.operationsToday(listOf(endsAtMidnight, startsAtMidnight, crossesMidnight), atMidnight, zone).map { it.id })
    }

    @Test
    fun pausedIsActionableButDoesNotBeatInProgress() {
        val paused = op("paused", "PAUSED", "2026-03-10T08:00:00-03:00")
        val planned = op("planned", "PLANNED", "2026-03-10T14:00:00-03:00")
        val running = op("running", "IN_PROGRESS", "2026-03-10T18:00:00-03:00", actualStart = "2026-03-10T07:00:00-03:00")
        assertEquals("paused", TodayOps.nextActionable(listOf(paused, planned), zone)?.id)
        assertEquals("paused", TodayOps.nextActionable(listOf(paused), zone)?.id)
        assertEquals("running", TodayOps.nextActionable(listOf(paused, planned, running), zone)?.id)
        assertEquals(listOf("paused"), TodayOps.operationsToday(listOf(paused), now, zone).map { it.id })
    }

    @Test
    fun nextActionableSkipsJustSyncedComplete() {
        val justDone = op("done-soon", "IN_PROGRESS", "2026-03-10T08:00:00-03:00")
        val next = op("next", "PLANNED", "2026-03-10T14:00:00-03:00")
        val queue = QueueState(
            items = listOf(
                QueuedCommand(
                    "c1",
                    "done-soon",
                    OpCommandType.COMPLETE,
                    createdAt = "2026-03-10T08:05:00Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-03-10T08:05:01Z",
                ),
            ),
        )
        val projected = listOf(justDone, next).map { it.withQueuedStatus(queue) }
        assertEquals("next", TodayOps.nextActionable(projected, zone)?.id)
        assertEquals("done-soon", TodayOps.nextActionable(listOf(justDone, next), zone)?.id)
    }

    @Test
    fun emptyInputsYieldEmptyToday() {
        assertEquals(emptyList<OperationDto>(), TodayOps.operationsToday(emptyList(), now, zone))
        assertNull(TodayOps.nextActionable(emptyList(), zone))
        val kpis = TodayOps.todayKpis(emptyList(), emptyList(), now, zone)
        assertEquals(0, kpis.opsToday)
        assertEquals(0, kpis.completedToday)
        assertEquals(0, kpis.openAlerts)
        assertNull(kpis.completedLabel)
    }
}
