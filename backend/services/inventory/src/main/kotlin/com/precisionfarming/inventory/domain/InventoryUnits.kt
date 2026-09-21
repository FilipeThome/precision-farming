package com.precisionfarming.inventory.domain

import com.precisionfarming.common.ConflictException
import java.math.BigDecimal
import java.math.RoundingMode

data class StockUnit(val unit: String, val quantity: BigDecimal, val reserved: BigDecimal)

object InventoryUnits {
    private val mass = mapOf(
        "G" to BigDecimal("0.001"),
        "KG" to BigDecimal.ONE,
        "T" to BigDecimal("1000"),
        "TON" to BigDecimal("1000"),
        "TONNE" to BigDecimal("1000"),
    )
    private val volume = mapOf(
        "ML" to BigDecimal("0.001"),
        "L" to BigDecimal.ONE,
        "M3" to BigDecimal("1000"),
    )
    private val count = mapOf(
        "UN" to BigDecimal.ONE,
        "UNIT" to BigDecimal.ONE,
        "PC" to BigDecimal.ONE,
    )

    fun apply(current: String, requested: String, quantity: BigDecimal, reserved: BigDecimal): StockUnit {
        val from = current.trim().uppercase()
        val to = requested.trim().uppercase()
        if (from == to) return StockUnit(to, quantity, reserved)
        val fromFactor = factor(from)
        val toFactor = factor(to)
        if (fromFactor == null || toFactor == null || fromFactor.first != toFactor.first) {
            throw ConflictException(
                "UNIT_CHANGE_UNSUPPORTED",
                "Cannot convert $current to $requested without a quantity movement",
            )
        }
        return StockUnit(
            to,
            convert(quantity, fromFactor.second, toFactor.second),
            convert(reserved, fromFactor.second, toFactor.second),
        )
    }

    private fun factor(unit: String): Pair<String, BigDecimal>? =
        mass[unit]?.let { "MASS" to it }
            ?: volume[unit]?.let { "VOL" to it }
            ?: count[unit]?.let { "COUNT" to it }

    private fun convert(amount: BigDecimal, from: BigDecimal, to: BigDecimal): BigDecimal =
        amount.multiply(from).divide(to, 6, RoundingMode.HALF_UP).stripTrailingZeros()
}
