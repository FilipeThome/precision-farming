package com.precisionfarming.harvest

import com.precisionfarming.common.DomainException
import com.precisionfarming.harvest.domain.HarvestWindow
import com.precisionfarming.harvest.domain.StorageOccupancy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

class HarvestDomainTest {
    @Test
    fun occupancySumsLotTons() {
        assertEquals(BigDecimal.ZERO, StorageOccupancy.usedT(emptyList()))
        assertEquals(BigDecimal("15"), StorageOccupancy.usedT(listOf(BigDecimal("10"), BigDecimal("5"))))
    }

    @Test
    fun occupancyRejectsOverflow() {
        val ex = assertThrows(DomainException::class.java) {
            StorageOccupancy.requireFits(BigDecimal("10"), BigDecimal("11"))
        }
        assertEquals("STORAGE_CAPACITY_INVALID", ex.code)
    }

    @Test
    fun windowDefaultsWhenBothNull() {
        val now = Instant.parse("2026-09-21T12:00:00Z")
        val window = HarvestWindow.resolve(null, null, now)
        assertEquals(now.plus(1, ChronoUnit.DAYS), window.start)
        assertEquals(now.plus(5, ChronoUnit.DAYS), window.end)
    }

    @Test
    fun windowRejectsExactlyOneDate() {
        val now = Instant.parse("2026-09-21T12:00:00Z")
        val startOnly = assertThrows(DomainException::class.java) {
            HarvestWindow.resolve(now, null, now)
        }
        assertEquals("HARVEST_WINDOW_INVALID", startOnly.code)
        val endOnly = assertThrows(DomainException::class.java) {
            HarvestWindow.resolve(null, now.plus(1, ChronoUnit.DAYS), now)
        }
        assertEquals("HARVEST_WINDOW_INVALID", endOnly.code)
    }

    @Test
    fun windowHonorsBothDatesWithinMax() {
        val now = Instant.parse("2026-09-21T12:00:00Z")
        val start = now.plus(2, ChronoUnit.DAYS)
        val end = start.plus(180, ChronoUnit.DAYS)
        val window = HarvestWindow.resolve(start, end, now)
        assertEquals(start, window.start)
        assertEquals(end, window.end)
    }

    @Test
    fun windowRejectsInvertedOrTooLong() {
        val now = Instant.parse("2026-09-21T12:00:00Z")
        val inverted = assertThrows(DomainException::class.java) {
            HarvestWindow.resolve(now.plus(2, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), now)
        }
        assertEquals("HARVEST_WINDOW_INVALID", inverted.code)
        val tooLong = assertThrows(DomainException::class.java) {
            HarvestWindow.resolve(now, now.plus(181, ChronoUnit.DAYS), now)
        }
        assertEquals("HARVEST_WINDOW_INVALID", tooLong.code)
    }
}
