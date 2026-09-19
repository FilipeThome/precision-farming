package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.WeatherWindowDto
import com.precisionfarming.mobile.data.favorableWindowUntil
import com.precisionfarming.mobile.data.fleetAvailability
import com.precisionfarming.mobile.data.openCriticalAlerts
import com.precisionfarming.mobile.data.ratio
import com.precisionfarming.mobile.data.traceabilityRatio
import com.precisionfarming.mobile.data.withinWindowRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NorthStarTest {
    private fun op(over: OperationDto.() -> OperationDto = { this }) = OperationDto(
        id = "op",
        fieldId = "f",
        farmId = "farm",
        type = "SPRAYING",
        status = "COMPLETED",
    ).over()

    @Test
    fun withinWindowOnTimeVsLate() {
        val ops = listOf(
            op { copy(id = "a", plannedEnd = "2026-09-10T12:00:00Z", actualEnd = "2026-09-10T11:30:00Z") },
            op { copy(id = "b", plannedEnd = "2026-09-10T12:00:00Z", actualEnd = "2026-09-10T13:00:00Z") },
            op { copy(id = "c", status = "IN_PROGRESS", plannedEnd = "2026-09-10T12:00:00Z", actualEnd = "2026-09-10T11:00:00Z") },
        )
        val r = withinWindowRatio(ops)
        assertEquals(1, r.numerator)
        assertEquals(2, r.denominator)
        assertEquals(50, r.pct)
    }

    @Test
    fun ratioPct() {
        assertNull(ratio(0, 0).pct)
        assertEquals(0, ratio(0, 5).pct)
        assertEquals(33, ratio(1, 3).pct)
        assertEquals(67, ratio(2, 3).pct)
        assertEquals(100, ratio(5, 5).pct)
    }

    @Test
    fun traceabilityAndFleetAndAlerts() {
        assertEquals(33, traceabilityRatio(listOf(op { copy(machineId = "m", itemId = "i") }, op { copy(machineId = "m") }, op())).pct)
        assertEquals(
            1,
            openCriticalAlerts(
                listOf(
                    AlertDto(id = "1", title = "A", severity = "CRITICAL", status = "OPEN"),
                    AlertDto(id = "2", title = "B", severity = "CRITICAL", status = "ACKED"),
                    AlertDto(id = "3", title = "C", severity = "WARNING", status = "OPEN"),
                ),
            ),
        )
        assertEquals(
            67,
            fleetAvailability(
                listOf(
                    MachineDto(id = "1", name = "a", status = "OPERATING", type = "T"),
                    MachineDto(id = "2", name = "b", status = "IDLE", type = "T"),
                    MachineDto(id = "3", name = "c", status = "MAINTENANCE", type = "T"),
                ),
            ).pct,
        )
    }

    @Test
    fun favorableWindowCoveringNow() {
        val now = java.time.Instant.parse("2026-09-10T10:00:00Z").toEpochMilli()
        val window = WeatherWindowDto(
            id = "w",
            farmId = "farm",
            windowType = "SPRAY",
            startAt = "2026-09-10T08:00:00Z",
            endAt = "2026-09-10T11:30:00Z",
            rating = "FAVORABLE",
        )
        assertEquals("2026-09-10T11:30:00Z", favorableWindowUntil(listOf(window), now))
        assertNull(favorableWindowUntil(listOf(window.copy(rating = "MARGINAL")), now))
        assertNull(favorableWindowUntil(listOf(window.copy(endAt = "2026-09-10T10:00:00Z")), now))
    }

    @Test
    fun withinWindowAcceptsOffsetDateTimes() {
        val ops = listOf(
            op {
                copy(
                    id = "a",
                    plannedEnd = "2026-09-10T12:00:00+00:00",
                    actualEnd = "2026-09-10T11:30:00+00:00",
                )
            },
        )
        assertEquals(100, withinWindowRatio(ops).pct)
    }
}
