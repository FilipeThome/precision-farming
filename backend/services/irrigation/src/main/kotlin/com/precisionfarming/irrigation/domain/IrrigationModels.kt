package com.precisionfarming.irrigation.domain

import java.math.BigDecimal
import java.util.UUID

data class SimulationInput(val farmId: UUID, val fieldId: UUID, val mm: BigDecimal, val areaHa: BigDecimal = BigDecimal("100"))

data class SimulationResult(
    val durationH: BigDecimal,
    val waterM3: BigDecimal,
    val estimatedCost: BigDecimal,
)

interface IrrigationSimulator {
    fun simulate(input: SimulationInput): SimulationResult
}
