package com.precisionfarming.reporting.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "report_operation_rows")
class ReportOperationEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val code: String,
    val type: String,
    val status: String,
)

@Entity
@Table(name = "report_inventory_rows")
class ReportInventoryEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val code: String,
    val name: String,
    val quantity: BigDecimal,
)

interface ReportOperationJpaRepository : JpaRepository<ReportOperationEntity, UUID> {
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<ReportOperationEntity>
}

interface ReportInventoryJpaRepository : JpaRepository<ReportInventoryEntity, UUID> {
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<ReportInventoryEntity>
}
