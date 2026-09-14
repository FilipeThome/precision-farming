package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TimeFormatTest {
    private val now = Instant.parse("2026-03-10T12:00:00Z")

    @Test
    fun formatAgeBuckets() {
        assertEquals("0 min", TimeFormat.formatAge("2026-03-10T11:59:40Z", now))
        assertEquals("4 min", TimeFormat.formatAge("2026-03-10T11:56:00Z", now))
        assertEquals("2h", TimeFormat.formatAge("2026-03-10T10:00:00Z", now))
        assertEquals("36h", TimeFormat.formatAge("2026-03-09T00:00:00Z", now))
        assertEquals("3d", TimeFormat.formatAge("2026-03-07T12:00:00Z", now))
        assertNull(TimeFormat.formatAge(null, now))
        assertNull(TimeFormat.formatAge("garbage", now))
        assertNull(TimeFormat.formatAge("2026-03-10T13:00:00Z", now)) // future
    }

    @Test
    fun staleThreshold() {
        assertFalse(TimeFormat.isStale("2026-03-10T10:00:00Z", now))
        assertTrue(TimeFormat.isStale("2026-03-09T00:00:00Z", now))
        assertTrue(TimeFormat.isStale("2026-03-10T10:00:00Z", now, Duration.ofHours(1)))
        assertFalse(TimeFormat.isStale(null, now))
    }

    @Test
    fun parseInstantIsTolerant() {
        val zone = ZoneId.of("UTC")
        assertEquals(now, TimeFormat.parseInstant("2026-03-10T12:00:00Z", zone))
        assertEquals(now, TimeFormat.parseInstant("2026-03-10T09:00:00-03:00", zone))
        assertEquals(now, TimeFormat.parseInstant("2026-03-10T12:00:00", zone))
        assertEquals(Instant.parse("2026-03-10T00:00:00Z"), TimeFormat.parseInstant("2026-03-10", zone))
        assertNull(TimeFormat.parseInstant("", zone))
        assertNull(TimeFormat.parseInstant("nope", zone))
    }

    @Test
    fun elapsedAndRemaining() {
        assertEquals("00:00:00", TimeFormat.formatElapsed(0))
        assertEquals("01:42:18", TimeFormat.formatElapsed(1 * 3600 + 42 * 60 + 18))
        assertEquals("26:00:05", TimeFormat.formatElapsed(26 * 3600 + 5))
        assertEquals("00:00:00", TimeFormat.formatElapsed(-10))
        assertEquals("1h 13min", TimeFormat.formatRemaining(now.plusSeconds(73 * 60), now))
        assertEquals("45 min", TimeFormat.formatRemaining(now.plusSeconds(45 * 60 + 30), now))
        assertEquals("2d 3h", TimeFormat.formatRemaining(now.plus(Duration.ofHours(51)), now))
        assertNull(TimeFormat.formatRemaining(now, now))
        assertNull(TimeFormat.formatRemaining(now.minusSeconds(1), now))
    }

    @Test
    fun clocksAndWindowsUseZone() {
        val sp = ZoneId.of("America/Sao_Paulo")
        assertEquals("09:00", TimeFormat.clock("2026-03-10T12:00:00Z", sp))
        assertEquals("09:00:05", TimeFormat.clockWithSeconds("2026-03-10T12:00:05Z", sp))
        assertEquals("06:00 – 10:30", TimeFormat.window("2026-03-10T09:00:00Z", "2026-03-10T13:30:00Z", sp))
        assertEquals("06:00 –", TimeFormat.window("2026-03-10T09:00:00Z", null, sp))
        assertEquals("– 10:30", TimeFormat.window(null, "2026-03-10T13:30:00Z", sp))
        assertNull(TimeFormat.window(null, null, sp))
        assertNull(TimeFormat.clock("garbage", sp))
        assertNull(TimeFormat.clock("", sp))
        assertNull(TimeFormat.clock(null, sp))
    }

    @Test
    fun formatAgeAndStaleBoundaries() {
        assertEquals("59 min", TimeFormat.formatAge("2026-03-10T11:01:00Z", now))
        assertEquals("1h", TimeFormat.formatAge("2026-03-10T11:00:00Z", now))
        assertEquals("47h", TimeFormat.formatAge("2026-03-08T13:00:00Z", now))
        assertEquals("2d", TimeFormat.formatAge("2026-03-08T12:00:00Z", now))
        assertEquals("0 min", TimeFormat.formatAge("2026-03-10T12:00:00Z", now))
        assertNull(TimeFormat.formatAge("", now))
        // stale is strictly older than the threshold
        assertFalse(TimeFormat.isStale("2026-03-09T12:00:00Z", now))
        assertTrue(TimeFormat.isStale("2026-03-09T11:59:59Z", now))
        assertEquals("1h 0min", TimeFormat.formatRemaining(now.plusSeconds(60 * 60), now))
        assertEquals("59 min", TimeFormat.formatRemaining(now.plusSeconds(59 * 60), now))
    }
}
