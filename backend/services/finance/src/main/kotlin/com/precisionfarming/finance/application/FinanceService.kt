package com.precisionfarming.finance.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.AccessScope
import com.precisionfarming.common.concurrency.VirtualJobs
import com.precisionfarming.finance.domain.MarketQuoteProvider
import com.precisionfarming.finance.domain.PnlSummary
import com.precisionfarming.finance.infrastructure.BudgetEntity
import com.precisionfarming.finance.infrastructure.BudgetJpaRepository
import com.precisionfarming.finance.infrastructure.CashflowEntity
import com.precisionfarming.finance.infrastructure.CashflowJpaRepository
import com.precisionfarming.finance.infrastructure.CostEntity
import com.precisionfarming.finance.infrastructure.CostJpaRepository
import com.precisionfarming.finance.infrastructure.MarketContractEntity
import com.precisionfarming.finance.infrastructure.MarketContractJpaRepository
import com.precisionfarming.finance.infrastructure.MarketExposureEntity
import com.precisionfarming.finance.infrastructure.MarketExposureJpaRepository
import com.precisionfarming.finance.infrastructure.MarketQuoteEntity
import com.precisionfarming.finance.infrastructure.MarketQuoteJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable

data class CostDto(
    val id: UUID, val farmId: UUID, val fieldId: UUID?, val category: String, val description: String,
    val amount: BigDecimal, val currency: String, val occurredAt: Instant,
)
data class BudgetDto(val id: UUID, val farmId: UUID, val seasonLabel: String, val category: String, val planned: BigDecimal, val actual: BigDecimal)
data class CashflowDto(val id: UUID, val farmId: UUID, val label: String, val direction: String, val amount: BigDecimal, val dueAt: Instant)
data class MarketQuoteDto(val id: UUID, val commodity: String, val exchange: String, val price: BigDecimal, val currency: String, val quotedAt: Instant)
data class MarketContractDto(
    val id: UUID, val farmId: UUID, val commodity: String, val volumeT: BigDecimal, val price: BigDecimal,
    val currency: String, val deliveryAt: Instant, val status: String,
)
data class MarketExposureDto(val id: UUID, val farmId: UUID, val commodity: String, val openT: BigDecimal, val hedgedT: BigDecimal, val riskScore: BigDecimal)

@Service
class FinanceService(
    private val costs: CostJpaRepository,
    private val budgets: BudgetJpaRepository,
    private val cashflows: CashflowJpaRepository,
    private val quotes: MarketQuoteJpaRepository,
    private val contracts: MarketContractJpaRepository,
    private val exposures: MarketExposureJpaRepository,
    private val market: MarketQuoteProvider,
) {
    fun listCosts(scope: AccessScope, farmId: UUID?) =
        costs.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun pnl(scope: AccessScope, farmId: UUID?): PnlSummary {
        val rows = costs.findByFarmIdIn(scope.resolveFarms(farmId))
        val revenue = rows.filter { it.category == "REVENUE" }.fold(BigDecimal.ZERO) { a, e -> a.add(e.amount) }
            .let { if (it.compareTo(BigDecimal.ZERO) == 0) BigDecimal("1850000") else it }
        val cost = rows.filter { it.category != "REVENUE" }.fold(BigDecimal.ZERO) { a, e -> a.add(e.amount) }
        val gross = revenue.subtract(cost)
        val margin = if (revenue.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
        else gross.multiply(BigDecimal("100")).divide(revenue, 2, RoundingMode.HALF_UP)
        return PnlSummary(revenue, cost, gross, margin)
    }

    fun listBudget(scope: AccessScope, farmId: UUID?) =
        budgets.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }
    fun listCashflow(scope: AccessScope, farmId: UUID?) =
        cashflows.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }
    fun listQuotes() = quotes.findAll().map { it.toDto() }
    fun listContracts(scope: AccessScope, farmId: UUID?) =
        contracts.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }
    fun listExposure(scope: AccessScope, farmId: UUID?) =
        exposures.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun seed() {
        if (costs.existsById(DemoIds.uuid("cost-001"))) return
        val now = Instant.now()
        val categories = listOf("SEED", "FERTILIZER", "CHEMICAL", "FUEL", "LABOR", "MAINTENANCE", "FREIGHT", "REVENUE")
        val farms = listOf("farm-001", "farm-002", "farm-003", "farm-004", "farm-005")
        val costRows = VirtualJobs.all(
            (1..50).map { i ->
                Callable {
                    val farm = farms[(i - 1) % farms.size]
                    val cat = categories[(i - 1) % categories.size]
                    val amount = if (cat == "REVENUE") BigDecimal("${50000 + i * 1200}") else BigDecimal("${800 + i * 37}")
                    CostEntity(
                        DemoIds.uuid("cost-%03d".format(i)), DemoIds.uuid(farm),
                        if (i % 2 == 0) DemoIds.uuid("field-%03d".format((i % 6) + 1)) else null,
                        cat, "Lançamento demo $i", amount, "BRL", now.minus(i.toLong(), ChronoUnit.DAYS),
                    )
                }
            },
        )
        costs.saveAll(costRows)
        budgets.saveAll(
            listOf(
                BudgetEntity(DemoIds.uuid("budget-001"), DemoIds.uuid("farm-001"), "2025/26", "FERTILIZER", BigDecimal("420000"), BigDecimal("388000")),
                BudgetEntity(DemoIds.uuid("budget-002"), DemoIds.uuid("farm-001"), "2025/26", "CHEMICAL", BigDecimal("210000"), BigDecimal("195000")),
                BudgetEntity(DemoIds.uuid("budget-003"), DemoIds.uuid("farm-002"), "2025/26", "SEED", BigDecimal("180000"), BigDecimal("176500")),
                BudgetEntity(DemoIds.uuid("budget-004"), DemoIds.uuid("farm-003"), "2025/26", "FUEL", BigDecimal("95000"), BigDecimal("101200")),
            ),
        )
        cashflows.saveAll(
            listOf(
                CashflowEntity(DemoIds.uuid("cf-001"), DemoIds.uuid("farm-001"), "Recebimento soja", "IN", BigDecimal("850000"), now.plus(15, ChronoUnit.DAYS)),
                CashflowEntity(DemoIds.uuid("cf-002"), DemoIds.uuid("farm-001"), "Pagamento insumos", "OUT", BigDecimal("220000"), now.plus(5, ChronoUnit.DAYS)),
                CashflowEntity(DemoIds.uuid("cf-003"), DemoIds.uuid("farm-002"), "Frete", "OUT", BigDecimal("48000"), now.plus(8, ChronoUnit.DAYS)),
                CashflowEntity(DemoIds.uuid("cf-004"), DemoIds.uuid("farm-003"), "Contrato milho", "IN", BigDecimal("610000"), now.plus(30, ChronoUnit.DAYS)),
            ),
        )
        quotes.saveAll(
            market.demoQuotes().mapIndexed { idx, triple ->
                val (commodity, exchange, price) = triple
                MarketQuoteEntity(
                    DemoIds.uuid("quote-%03d".format(idx + 1)), commodity, exchange, price,
                    if (exchange == "B3") "BRL" else "USD", now,
                )
            },
        )
        contracts.saveAll(
            listOf(
                MarketContractEntity(DemoIds.uuid("contract-001"), DemoIds.uuid("farm-001"), "SOY", BigDecimal("1200"), BigDecimal("138.20"), "BRL", now.plus(45, ChronoUnit.DAYS), "OPEN"),
                MarketContractEntity(DemoIds.uuid("contract-002"), DemoIds.uuid("farm-002"), "CORN", BigDecimal("800"), BigDecimal("72.50"), "BRL", now.plus(60, ChronoUnit.DAYS), "OPEN"),
                MarketContractEntity(DemoIds.uuid("contract-003"), DemoIds.uuid("farm-003"), "SOY", BigDecimal("2000"), BigDecimal("12.45"), "USD", now.plus(90, ChronoUnit.DAYS), "HEDGED"),
            ),
        )
        exposures.saveAll(
            listOf(
                MarketExposureEntity(DemoIds.uuid("exposure-001"), DemoIds.uuid("farm-001"), "SOY", BigDecimal("1800"), BigDecimal("1200"), BigDecimal("62.5")),
                MarketExposureEntity(DemoIds.uuid("exposure-002"), DemoIds.uuid("farm-002"), "CORN", BigDecimal("950"), BigDecimal("800"), BigDecimal("41.0")),
                MarketExposureEntity(DemoIds.uuid("exposure-003"), DemoIds.uuid("farm-003"), "SOY", BigDecimal("3200"), BigDecimal("2000"), BigDecimal("55.0")),
            ),
        )
    }

    private fun CostEntity.toDto() = CostDto(id, farmId, fieldId, category, description, amount, currency, occurredAt)
    private fun BudgetEntity.toDto() = BudgetDto(id, farmId, seasonLabel, category, planned, actual)
    private fun CashflowEntity.toDto() = CashflowDto(id, farmId, label, direction, amount, dueAt)
    private fun MarketQuoteEntity.toDto() = MarketQuoteDto(id, commodity, exchange, price, currency, quotedAt)
    private fun MarketContractEntity.toDto() = MarketContractDto(id, farmId, commodity, volumeT, price, currency, deliveryAt, status)
    private fun MarketExposureEntity.toDto() = MarketExposureDto(id, farmId, commodity, openT, hedgedT, riskScore)
}

@Service
class FinanceSeed(private val svc: FinanceService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedFinance() = ApplicationRunner { if (seed) svc.seed() }
}
