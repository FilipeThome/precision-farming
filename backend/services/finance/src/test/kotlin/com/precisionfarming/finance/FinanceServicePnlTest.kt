package com.precisionfarming.finance

import com.precisionfarming.common.DemoIds
import com.precisionfarming.finance.application.FinanceService
import com.precisionfarming.finance.domain.MarketQuoteProvider
import com.precisionfarming.finance.infrastructure.BudgetJpaRepository
import com.precisionfarming.finance.infrastructure.CashflowJpaRepository
import com.precisionfarming.finance.infrastructure.CostJpaRepository
import com.precisionfarming.finance.infrastructure.MarketContractJpaRepository
import com.precisionfarming.finance.infrastructure.MarketExposureJpaRepository
import com.precisionfarming.finance.infrastructure.MarketQuoteJpaRepository
import com.precisionfarming.security.AccessScope
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FinanceServicePnlTest {
    private val costs = mockk<CostJpaRepository>()
    private val budgets = mockk<BudgetJpaRepository>(relaxed = true)
    private val cashflows = mockk<CashflowJpaRepository>(relaxed = true)
    private val quotes = mockk<MarketQuoteJpaRepository>(relaxed = true)
    private val contracts = mockk<MarketContractJpaRepository>(relaxed = true)
    private val exposures = mockk<MarketExposureJpaRepository>(relaxed = true)
    private val market = mockk<MarketQuoteProvider>(relaxed = true)

    private val svc = FinanceService(costs, budgets, cashflows, quotes, contracts, exposures, market)

    private val farm1 = DemoIds.uuid("farm-001")
    private val farm2 = DemoIds.uuid("farm-002")
    private val tenant = DemoIds.uuid("tenant-demo")

    @Test
    fun `pnl returns one row per scoped farm when farmId is null`() {
        every { costs.sumAmountByFarmIdInAndCategory(setOf(farm1), "REVENUE") } returns BigDecimal("100")
        every { costs.sumAmountByFarmIdInAndCategoryNot(setOf(farm1), "REVENUE") } returns BigDecimal("40")
        every { costs.sumAmountByFarmIdInAndCategory(setOf(farm2), "REVENUE") } returns BigDecimal("200")
        every { costs.sumAmountByFarmIdInAndCategoryNot(setOf(farm2), "REVENUE") } returns BigDecimal("50")

        val scope = AccessScope(tenant, setOf(farm1, farm2), "ADMIN")
        val rows = svc.pnl(scope, null)

        assertEquals(2, rows.size)
        assertEquals(setOf(farm1, farm2), rows.map { it.farmId }.toSet())
        val r1 = rows.first { it.farmId == farm1 }
        assertEquals(0, r1.revenue.compareTo(BigDecimal("100")))
        assertEquals(0, r1.cost.compareTo(BigDecimal("40")))
        assertEquals(0, r1.margin.compareTo(BigDecimal("60")))
    }

    @Test
    fun `pnl filters to single farm when farmId requested`() {
        every { costs.sumAmountByFarmIdInAndCategory(setOf(farm1), "REVENUE") } returns BigDecimal("10")
        every { costs.sumAmountByFarmIdInAndCategoryNot(setOf(farm1), "REVENUE") } returns BigDecimal("3")

        val scope = AccessScope(tenant, setOf(farm1, farm2), "FARM_MANAGER")
        val rows = svc.pnl(scope, farm1)

        assertEquals(1, rows.size)
        assertEquals(farm1, rows.single().farmId)
        assertTrue(rows.single().margin.compareTo(BigDecimal("7")) == 0)
    }
}
