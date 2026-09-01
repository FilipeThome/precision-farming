package com.precisionfarming.finance.domain

import java.math.BigDecimal

data class PnlSummary(
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val grossMargin: BigDecimal,
    val marginPct: BigDecimal,
)

interface MarketQuoteProvider {
    fun demoQuotes(): List<Triple<String, String, BigDecimal>>
}
