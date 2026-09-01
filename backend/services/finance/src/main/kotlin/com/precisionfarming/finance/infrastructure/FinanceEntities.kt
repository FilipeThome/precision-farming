package com.precisionfarming.finance.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "cost_transactions")
class CostEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID?,
    val category: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    @Column(name = "occurred_at") val occurredAt: Instant,
)

@Entity
@Table(name = "budgets")
class BudgetEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "season_label") val seasonLabel: String,
    val category: String,
    val planned: BigDecimal,
    var actual: BigDecimal,
)

@Entity
@Table(name = "cashflow_entries")
class CashflowEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val label: String,
    val direction: String,
    val amount: BigDecimal,
    @Column(name = "due_at") val dueAt: Instant,
)

@Entity
@Table(name = "market_quotes")
class MarketQuoteEntity(
    @Id val id: UUID,
    val commodity: String,
    val exchange: String,
    val price: BigDecimal,
    val currency: String,
    @Column(name = "quoted_at") val quotedAt: Instant,
)

@Entity
@Table(name = "market_contracts")
class MarketContractEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val commodity: String,
    @Column(name = "volume_t") val volumeT: BigDecimal,
    val price: BigDecimal,
    val currency: String,
    @Column(name = "delivery_at") val deliveryAt: Instant,
    var status: String,
)

@Entity
@Table(name = "market_exposures")
class MarketExposureEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val commodity: String,
    @Column(name = "open_t") val openT: BigDecimal,
    @Column(name = "hedged_t") val hedgedT: BigDecimal,
    @Column(name = "risk_score") val riskScore: BigDecimal,
)

interface CostJpaRepository : JpaRepository<CostEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<CostEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<CostEntity>
}
interface BudgetJpaRepository : JpaRepository<BudgetEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<BudgetEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<BudgetEntity>
}
interface CashflowJpaRepository : JpaRepository<CashflowEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<CashflowEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<CashflowEntity>
}
interface MarketQuoteJpaRepository : JpaRepository<MarketQuoteEntity, UUID>
interface MarketContractJpaRepository : JpaRepository<MarketContractEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<MarketContractEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<MarketContractEntity>
}
interface MarketExposureJpaRepository : JpaRepository<MarketExposureEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<MarketExposureEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<MarketExposureEntity>
}
