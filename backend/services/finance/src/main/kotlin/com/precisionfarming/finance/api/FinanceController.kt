package com.precisionfarming.finance.api

import com.precisionfarming.finance.application.FinanceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class FinanceController(private val svc: FinanceService) {
    @GetMapping("/finance/costs")
    fun costs(@RequestParam(required = false) farmId: UUID?) = svc.listCosts(farmId)

    @GetMapping("/finance/pnl")
    fun pnl(@RequestParam(required = false) farmId: UUID?) = svc.pnl(farmId)

    @GetMapping("/finance/budget")
    fun budget(@RequestParam(required = false) farmId: UUID?) = svc.listBudget(farmId)

    @GetMapping("/finance/cashflow")
    fun cashflow(@RequestParam(required = false) farmId: UUID?) = svc.listCashflow(farmId)

    @GetMapping("/market/quotes")
    fun quotes() = svc.listQuotes()

    @GetMapping("/market/contracts")
    fun contracts(@RequestParam(required = false) farmId: UUID?) = svc.listContracts(farmId)

    @GetMapping("/market/exposure")
    fun exposure(@RequestParam(required = false) farmId: UUID?) = svc.listExposure(farmId)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class FinanceSeedController(private val svc: FinanceService) {
    @org.springframework.web.bind.annotation.PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "finance").also { svc.seed() }
}
