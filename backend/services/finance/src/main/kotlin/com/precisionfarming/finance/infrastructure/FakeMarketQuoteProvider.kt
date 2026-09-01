package com.precisionfarming.finance.infrastructure

import com.precisionfarming.finance.domain.MarketQuoteProvider
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FakeMarketQuoteProvider : MarketQuoteProvider {
    override fun demoQuotes(): List<Triple<String, String, BigDecimal>> = listOf(
        Triple("SOY", "CBOT", BigDecimal("12.45")),
        Triple("CORN", "CBOT", BigDecimal("4.82")),
        Triple("SOY", "B3", BigDecimal("138.20")),
        Triple("CORN", "B3", BigDecimal("72.50")),
    )
}
