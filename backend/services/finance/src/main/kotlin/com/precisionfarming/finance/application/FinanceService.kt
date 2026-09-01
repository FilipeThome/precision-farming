package com.precisionfarming.finance.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.AccessScope
import com.precisionfarming.common.concurrency.VirtualJobs
import com.precisionfarming.finance.domain.MarketQuoteProvider
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
data class PnlRowDto(
    val id: String,
    val farmId: UUID?,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val margin: BigDecimal,
    val currency: String = "BRL",
    val period: String = "YTD",
)

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

    fun pnl(scope: AccessScope, farmId: UUID?): List<PnlRowDto> {
        val farms = scope.resolveFarms(farmId).sortedBy { it.toString() }
        return farms.map { fid ->
            val revenue = costs.sumAmountByFarmIdInAndCategory(setOf(fid), "REVENUE")
            val cost = costs.sumAmountByFarmIdInAndCategoryNot(setOf(fid), "REVENUE")
            PnlRowDto(
                id = "pnl-$fid",
                farmId = fid,
                revenue = revenue,
                cost = cost,
                margin = revenue.subtract(cost),
            )
        }
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
        val now = Instant.now()
        val categories = listOf("SEED", "FERTILIZER", "CHEMICAL", "FUEL", "LABOR", "MAINTENANCE", "FREIGHT", "REVENUE")
        val farms = (1..8).map { "farm-%03d".format(it) }
        val fieldsByFarm = mapOf(
            "farm-001" to listOf("field-001", "field-002", "field-003", "field-014"),
            "farm-002" to listOf("field-004", "field-005", "field-015"),
            "farm-003" to listOf("field-006", "field-007", "field-008"),
            "farm-004" to listOf("field-009", "field-010", "field-011"),
            "farm-005" to listOf("field-012", "field-013", "field-016"),
            "farm-006" to listOf("field-017", "field-018"),
            "farm-007" to listOf("field-019", "field-020"),
            "farm-008" to listOf("field-021", "field-022"),
        )
        val costRows = VirtualJobs.all(
            (1..56).map { i ->
                Callable {
                    val farm = farms[(i - 1) % farms.size]
                    val cat = categories[(i - 1) % categories.size]
                    val amount = if (cat == "REVENUE") BigDecimal("${50000 + i * 1200}") else BigDecimal("${800 + i * 37}")
                    val farmFields = fieldsByFarm.getValue(farm)
                    val fieldId = if (i % 2 == 0) DemoIds.uuid(farmFields[(i - 1) % farmFields.size]) else null
                    CostEntity(
                        DemoIds.uuid("cost-%03d".format(i)), DemoIds.uuid(farm),
                        fieldId,
                        cat, "Lançamento demo $i", amount, "BRL", now.minus(i.toLong(), ChronoUnit.DAYS),
                    )
                }
            },
        )
        val existingCosts = costs.findAllById(costRows.map { it.id }).map { it.id }.toHashSet()
        costs.saveAll(costRows.filter { it.id !in existingCosts })

        val budgetRows = listOf(
            BudgetEntity(DemoIds.uuid("budget-001"), DemoIds.uuid("farm-001"), "2025/26", "FERTILIZER", BigDecimal("420000"), BigDecimal("388000")),
            BudgetEntity(DemoIds.uuid("budget-002"), DemoIds.uuid("farm-001"), "2025/26", "CHEMICAL", BigDecimal("210000"), BigDecimal("195000")),
            BudgetEntity(DemoIds.uuid("budget-003"), DemoIds.uuid("farm-002"), "2025/26", "SEED", BigDecimal("180000"), BigDecimal("176500")),
            BudgetEntity(DemoIds.uuid("budget-004"), DemoIds.uuid("farm-003"), "2025/26", "FUEL", BigDecimal("95000"), BigDecimal("101200")),
            BudgetEntity(DemoIds.uuid("budget-005"), DemoIds.uuid("farm-006"), "2025/26", "FERTILIZER", BigDecimal("260000"), BigDecimal("241000")),
            BudgetEntity(DemoIds.uuid("budget-006"), DemoIds.uuid("farm-008"), "2025/26", "CHEMICAL", BigDecimal("120000"), BigDecimal("112500")),
            BudgetEntity(DemoIds.uuid("budget-007"), DemoIds.uuid("farm-001"), "2025/26", "FUEL", BigDecimal("145000"), BigDecimal("151800")),
            BudgetEntity(DemoIds.uuid("budget-008"), DemoIds.uuid("farm-001"), "2025/26", "LABOR", BigDecimal("320000"), BigDecimal("298500")),
            BudgetEntity(DemoIds.uuid("budget-009"), DemoIds.uuid("farm-001"), "2025/26", "SEED", BigDecimal("195000"), BigDecimal("188200")),
            BudgetEntity(DemoIds.uuid("budget-010"), DemoIds.uuid("farm-004"), "2025/26", "MAINTENANCE", BigDecimal("88000"), BigDecimal("92400")),
            BudgetEntity(DemoIds.uuid("budget-011"), DemoIds.uuid("farm-005"), "2025/26", "FERTILIZER", BigDecimal("155000"), BigDecimal("149000")),
            BudgetEntity(DemoIds.uuid("budget-012"), DemoIds.uuid("farm-007"), "2025/26", "FUEL", BigDecimal("76000"), BigDecimal("71200")),
        )
        val existingBudgets = budgets.findAllById(budgetRows.map { it.id }).map { it.id }.toHashSet()
        budgets.saveAll(budgetRows.filter { it.id !in existingBudgets })

        val cashflowRows = listOf(
            CashflowEntity(DemoIds.uuid("cf-001"), DemoIds.uuid("farm-001"), "Recebimento soja", "IN", BigDecimal("850000"), now.plus(15, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-002"), DemoIds.uuid("farm-001"), "Pagamento insumos", "OUT", BigDecimal("220000"), now.plus(5, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-003"), DemoIds.uuid("farm-002"), "Frete", "OUT", BigDecimal("48000"), now.plus(8, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-004"), DemoIds.uuid("farm-003"), "Contrato milho", "IN", BigDecimal("610000"), now.plus(30, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-005"), DemoIds.uuid("farm-006"), "Recebimento soja", "IN", BigDecimal("420000"), now.plus(20, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-006"), DemoIds.uuid("farm-001"), "Parcela financiamento", "OUT", BigDecimal("95000"), now.minus(25, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-007"), DemoIds.uuid("farm-001"), "Adiantamento trading", "IN", BigDecimal("310000"), now.minus(18, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-008"), DemoIds.uuid("farm-001"), "Folha operacional", "OUT", BigDecimal("128000"), now.minus(10, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-009"), DemoIds.uuid("farm-001"), "Venda milho", "IN", BigDecimal("275000"), now.minus(3, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-010"), DemoIds.uuid("farm-001"), "Manutenção frota", "OUT", BigDecimal("42000"), now.plus(12, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-011"), DemoIds.uuid("farm-001"), "Arrendamento", "OUT", BigDecimal("180000"), now.plus(22, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-012"), DemoIds.uuid("farm-004"), "Recebimento soja", "IN", BigDecimal("390000"), now.plus(18, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-013"), DemoIds.uuid("farm-005"), "Combustível", "OUT", BigDecimal("56000"), now.plus(6, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-014"), DemoIds.uuid("farm-007"), "Contrato algodão", "IN", BigDecimal("510000"), now.plus(40, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-015"), DemoIds.uuid("farm-008"), "Impostos e taxas", "OUT", BigDecimal("34000"), now.plus(9, ChronoUnit.DAYS)),
            CashflowEntity(DemoIds.uuid("cf-016"), DemoIds.uuid("farm-002"), "Recebimento milho", "IN", BigDecimal("245000"), now.minus(7, ChronoUnit.DAYS)),
        )
        val existingCf = cashflows.findAllById(cashflowRows.map { it.id }).map { it.id }.toHashSet()
        cashflows.saveAll(cashflowRows.filter { it.id !in existingCf })

        val quoteRows = market.demoQuotes().mapIndexed { idx, triple ->
            val (commodity, exchange, price) = triple
            MarketQuoteEntity(
                DemoIds.uuid("quote-%03d".format(idx + 1)), commodity, exchange, price,
                if (exchange == "B3") "BRL" else "USD", now,
            )
        }
        val existingQuotes = quotes.findAllById(quoteRows.map { it.id }).map { it.id }.toHashSet()
        quotes.saveAll(quoteRows.filter { it.id !in existingQuotes })

        val contractRows = listOf(
            MarketContractEntity(DemoIds.uuid("contract-001"), DemoIds.uuid("farm-001"), "SOY", BigDecimal("1200"), BigDecimal("138.20"), "BRL", now.plus(45, ChronoUnit.DAYS), "OPEN"),
            MarketContractEntity(DemoIds.uuid("contract-002"), DemoIds.uuid("farm-002"), "CORN", BigDecimal("800"), BigDecimal("72.50"), "BRL", now.plus(60, ChronoUnit.DAYS), "OPEN"),
            MarketContractEntity(DemoIds.uuid("contract-003"), DemoIds.uuid("farm-003"), "SOY", BigDecimal("2000"), BigDecimal("12.45"), "USD", now.plus(90, ChronoUnit.DAYS), "HEDGED"),
            MarketContractEntity(DemoIds.uuid("contract-004"), DemoIds.uuid("farm-007"), "SOY", BigDecimal("900"), BigDecimal("137.50"), "BRL", now.plus(50, ChronoUnit.DAYS), "OPEN"),
        )
        val existingContracts = contracts.findAllById(contractRows.map { it.id }).map { it.id }.toHashSet()
        contracts.saveAll(contractRows.filter { it.id !in existingContracts })

        val exposureRows = listOf(
            MarketExposureEntity(DemoIds.uuid("exposure-001"), DemoIds.uuid("farm-001"), "SOY", BigDecimal("1800"), BigDecimal("1200"), BigDecimal("62.5")),
            MarketExposureEntity(DemoIds.uuid("exposure-002"), DemoIds.uuid("farm-002"), "CORN", BigDecimal("950"), BigDecimal("800"), BigDecimal("41.0")),
            MarketExposureEntity(DemoIds.uuid("exposure-003"), DemoIds.uuid("farm-003"), "SOY", BigDecimal("3200"), BigDecimal("2000"), BigDecimal("55.0")),
            MarketExposureEntity(DemoIds.uuid("exposure-004"), DemoIds.uuid("farm-006"), "SOY", BigDecimal("1400"), BigDecimal("600"), BigDecimal("58.0")),
            MarketExposureEntity(DemoIds.uuid("exposure-005"), DemoIds.uuid("farm-008"), "CORN", BigDecimal("700"), BigDecimal("300"), BigDecimal("48.5")),
        )
        val existingExposures = exposures.findAllById(exposureRows.map { it.id }).map { it.id }.toHashSet()
        exposures.saveAll(exposureRows.filter { it.id !in existingExposures })
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
