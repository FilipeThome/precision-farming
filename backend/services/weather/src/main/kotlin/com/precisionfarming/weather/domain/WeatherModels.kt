package com.precisionfarming.weather.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.MonthDay

enum class PlantingDecision { ALLOWED, BLOCKED }

enum class PlantingBlockReason { ZARC_OUT_OF_WINDOW, SANITARY_VOID }

data class PlantingGateDecision(
    val decision: PlantingDecision,
    val reason: PlantingBlockReason?,
)

data class MonthDayWindow(val start: MonthDay, val end: MonthDay) {
    /** Inclusive year-agnostic window; handles ranges that wrap year-end. */
    fun contains(date: LocalDate): Boolean {
        val md = MonthDay.from(date)
        return if (!start.isAfter(end)) {
            !md.isBefore(start) && !md.isAfter(end)
        } else {
            !md.isBefore(start) || !md.isAfter(end)
        }
    }
}

/**
 * Sanitary void wins if both match.
 * ZARC window Oct 1–Dec 20; sanitary void Jun 15–Sep 15 (year-agnostic).
 */
fun plantingGateDecision(date: LocalDate, zarc: MonthDayWindow, sanitaryVoid: MonthDayWindow): PlantingGateDecision {
    if (sanitaryVoid.contains(date)) {
        return PlantingGateDecision(PlantingDecision.BLOCKED, PlantingBlockReason.SANITARY_VOID)
    }
    if (!zarc.contains(date)) {
        return PlantingGateDecision(PlantingDecision.BLOCKED, PlantingBlockReason.ZARC_OUT_OF_WINDOW)
    }
    return PlantingGateDecision(PlantingDecision.ALLOWED, null)
}

/** Deterministic deficit from rain already stored in forecasts. */
fun waterDeficitMm(rainMm: BigDecimal): BigDecimal {
    val baseline = BigDecimal("5.0")
    val deficit = baseline.subtract(rainMm).max(BigDecimal.ZERO)
    return deficit.setScale(2, RoundingMode.HALF_UP)
}

/** Frost risk from min temperature already stored. */
fun frostRisk(temperatureMin: BigDecimal): Boolean = temperatureMin < BigDecimal("5")
