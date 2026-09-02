package com.precisionfarming.irrigation.infrastructure

import com.precisionfarming.irrigation.domain.IrrigationSimulator
import com.precisionfarming.irrigation.domain.SimulationInput
import com.precisionfarming.irrigation.domain.SimulationResult
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class FakeIrrigationSimulator : IrrigationSimulator {
    override fun simulate(input: SimulationInput): SimulationResult {
        val water = input.mm.multiply(input.areaHa).multiply(BigDecimal("10")).setScale(2, RoundingMode.HALF_UP)
        val duration = input.mm.divide(BigDecimal("8"), 2, RoundingMode.HALF_UP)
        val cost = water.multiply(BigDecimal("0.45")).setScale(2, RoundingMode.HALF_UP)
        return SimulationResult(duration, water, cost)
    }
}
