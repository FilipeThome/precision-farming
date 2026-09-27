package com.precisionfarming.weather

import com.precisionfarming.common.DemoIds
import com.precisionfarming.weather.domain.MonthDayWindow
import com.precisionfarming.weather.domain.PlantingBlockReason
import com.precisionfarming.weather.domain.PlantingDecision
import com.precisionfarming.weather.domain.frostRisk
import com.precisionfarming.weather.domain.plantingGateDecision
import com.precisionfarming.weather.domain.waterDeficitMm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.MonthDay

class WeatherDomainTest {
    private val zarc = MonthDayWindow(MonthDay.of(10, 1), MonthDay.of(12, 20))
    private val sanitary = MonthDayWindow(MonthDay.of(6, 15), MonthDay.of(9, 15))

    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("weather-001"), DemoIds.uuid("weather-001"))
    }

    @Test
    fun plantingAllowedInsideOctDec() {
        val r = plantingGateDecision(LocalDate.of(2026, 11, 10), zarc, sanitary)
        assertEquals(PlantingDecision.ALLOWED, r.decision)
        assertNull(r.reason)
    }

    @Test
    fun plantingZarcOutInJanuary() {
        val r = plantingGateDecision(LocalDate.of(2026, 1, 15), zarc, sanitary)
        assertEquals(PlantingDecision.BLOCKED, r.decision)
        assertEquals(PlantingBlockReason.ZARC_OUT_OF_WINDOW, r.reason)
    }

    @Test
    fun plantingSanitaryVoidInJuly() {
        val r = plantingGateDecision(LocalDate.of(2026, 7, 1), zarc, sanitary)
        assertEquals(PlantingDecision.BLOCKED, r.decision)
        assertEquals(PlantingBlockReason.SANITARY_VOID, r.reason)
    }

    @Test
    fun parametricDeficitAndFrostAreDeterministic() {
        assertEquals(0, BigDecimal("0.20").compareTo(waterDeficitMm(BigDecimal("4.8"))))
        assertEquals(0, BigDecimal("4.80").compareTo(waterDeficitMm(BigDecimal("0.2"))))
        assertFalse(frostRisk(BigDecimal("18")))
        assertTrue(frostRisk(BigDecimal("4")))
    }
}
