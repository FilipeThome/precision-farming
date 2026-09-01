package com.precisionfarming.finance.api

import com.precisionfarming.finance.application.FinanceService
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class FinanceController(
    private val svc: FinanceService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/finance/costs")
    fun costs(@RequestParam(required = false) farmId: UUID?) = svc.listCosts(farmAccess.current(), farmId)

    @GetMapping("/finance/pnl")
    fun pnl(@RequestParam(required = false) farmId: UUID?) = svc.pnl(farmAccess.current(), farmId)

    @GetMapping("/finance/budget")
    fun budget(@RequestParam(required = false) farmId: UUID?) = svc.listBudget(farmAccess.current(), farmId)

    @GetMapping("/finance/cashflow")
    fun cashflow(@RequestParam(required = false) farmId: UUID?) = svc.listCashflow(farmAccess.current(), farmId)

    @GetMapping("/market/quotes")
    fun quotes() = svc.listQuotes()

    @GetMapping("/market/contracts")
    fun contracts(@RequestParam(required = false) farmId: UUID?) = svc.listContracts(farmAccess.current(), farmId)

    @GetMapping("/market/exposure")
    fun exposure(@RequestParam(required = false) farmId: UUID?) = svc.listExposure(farmAccess.current(), farmId)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class FinanceSeedController(private val svc: FinanceService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "finance").also { svc.seed() }
}
