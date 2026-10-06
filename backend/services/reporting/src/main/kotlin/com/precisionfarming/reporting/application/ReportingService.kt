package com.precisionfarming.reporting.application

import com.precisionfarming.common.DemoCatalog
import com.precisionfarming.common.DemoIds
import com.precisionfarming.reporting.infrastructure.ReportInventoryEntity
import com.precisionfarming.reporting.infrastructure.ReportInventoryJpaRepository
import com.precisionfarming.reporting.infrastructure.ReportOperationEntity
import com.precisionfarming.reporting.infrastructure.ReportOperationJpaRepository
import com.precisionfarming.security.AccessScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Demo snapshot of operations/inventory for CSV export. Not fed by live service events.
 */
@Service
class ReportingService(
    private val operations: ReportOperationJpaRepository,
    private val inventory: ReportInventoryJpaRepository,
) {
    private val pdf = ReportPdfRenderer()

    fun operationsCsv(scope: AccessScope, farmId: UUID?): String {
        val rows = operationRows(scope, farmId)
        return buildString {
            appendLine("id,type,status")
            rows.forEach { appendLine("${csvCell(it.code)},${csvCell(it.type)},${csvCell(it.status)}") }
        }
    }

    fun inventoryCsv(scope: AccessScope, farmId: UUID?): String {
        val rows = inventoryRows(scope, farmId)
        return buildString {
            appendLine("id,name,quantity")
            rows.forEach {
                appendLine("${csvCell(it.code)},${csvCell(it.name)},${csvCell(it.quantity.stripTrailingZeros().toPlainString())}")
            }
        }
    }

    fun reportCatalog(): List<ReportCatalogItemDto> = listOf(
        ReportCatalogItemDto("operations", "Operacoes", "PDF", "operations.pdf", "/api/v1/reports/operations.pdf"),
        ReportCatalogItemDto("inventory", "Estoque", "PDF", "inventory.pdf", "/api/v1/reports/inventory.pdf"),
    )

    fun operationsPdf(scope: AccessScope, farmId: UUID?): ByteArray {
        val rows = operationRows(scope, farmId)
        return pdf.render(
            title = "Relatorio de Operacoes",
            subtitle = subtitle(scope, farmId),
            generatedAt = DEMO_REPORT_NOW,
            headers = listOf("Codigo", "Tipo", "Status"),
            rows = rows.map { listOf(it.code, it.type, it.status) },
        )
    }

    fun inventoryPdf(scope: AccessScope, farmId: UUID?): ByteArray {
        val rows = inventoryRows(scope, farmId)
        return pdf.render(
            title = "Relatorio de Estoque",
            subtitle = subtitle(scope, farmId),
            generatedAt = DEMO_REPORT_NOW,
            headers = listOf("Codigo", "Item", "Quantidade"),
            rows = rows.map { listOf(it.code, it.name, it.quantity.stripTrailingZeros().toPlainString()) },
        )
    }

    @Transactional
    fun seed() {
        operations.saveAll(demoOperations())
        inventory.saveAll(demoInventory())
    }

    companion object {
        private val DEMO_REPORT_NOW: Instant = Instant.parse("2026-09-03T12:00:00Z")

        fun csvCell(value: String): String {
            val formulaIdx = value.indexOfFirst { it != ' ' && it != '\t' }
            val formula = formulaIdx >= 0 && value[formulaIdx] in "=+-@\t\r"
            val escaped = if (formula) value.substring(0, formulaIdx) + "'" + value.substring(formulaIdx) else value
            val needsQuote = formula || escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
            if (!needsQuote) return escaped
            return "\"${escaped.replace("\"", "\"\"")}\""
        }

        fun demoOperations(): List<ReportOperationEntity> = listOf(
            ReportOperationEntity(DemoIds.uuid("rpt-op-001"), DemoIds.uuid("farm-001"), "op-001", "PLANTING", "COMPLETED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-002"), DemoIds.uuid("farm-001"), "op-002", "SPRAYING", "IN_PROGRESS"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-003"), DemoIds.uuid("farm-001"), "op-003", "FERTILIZING", "PLANNED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-004"), DemoIds.uuid("farm-001"), "op-004", "INSPECTION", "PLANNED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-005"), DemoIds.uuid("farm-002"), "op-005", "PLANTING", "PAUSED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-006"), DemoIds.uuid("farm-002"), "op-006", "SPRAYING", "IN_PROGRESS"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-007"), DemoIds.uuid("farm-003"), "op-007", "PLANTING", "COMPLETED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-008"), DemoIds.uuid("farm-003"), "op-008", "FERTILIZING", "PAUSED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-009"), DemoIds.uuid("farm-003"), "op-009", "SPRAYING", "PLANNED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-010"), DemoIds.uuid("farm-004"), "op-010", "PLANTING", "IN_PROGRESS"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-011"), DemoIds.uuid("farm-004"), "op-011", "HARVEST", "PLANNED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-012"), DemoIds.uuid("farm-004"), "op-012", "FERTILIZING", "COMPLETED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-013"), DemoIds.uuid("farm-005"), "op-013", "PLANTING", "PLANNED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-014"), DemoIds.uuid("farm-005"), "op-014", "SPRAYING", "PAUSED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-015"), DemoIds.uuid("farm-005"), "op-015", "INSPECTION", "COMPLETED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-016"), DemoIds.uuid("farm-006"), "op-016", "PLANTING", "IN_PROGRESS"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-017"), DemoIds.uuid("farm-006"), "op-017", "FERTILIZING", "PLANNED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-018"), DemoIds.uuid("farm-007"), "op-018", "SPRAYING", "PAUSED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-019"), DemoIds.uuid("farm-007"), "op-019", "PLANTING", "COMPLETED"),
            ReportOperationEntity(DemoIds.uuid("rpt-op-020"), DemoIds.uuid("farm-008"), "op-020", "FERTILIZING", "IN_PROGRESS"),
        )

        fun demoInventory(): List<ReportInventoryEntity> = listOf(
            ReportInventoryEntity(DemoIds.uuid("rpt-item-001"), DemoIds.uuid("farm-001"), "item-001", "GLYPHOSATE", BigDecimal("420")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-002"), DemoIds.uuid("farm-001"), "item-002", "UREA", BigDecimal("1800")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-003"), DemoIds.uuid("farm-002"), "item-003", "SOY_SEED", BigDecimal("900")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-004"), DemoIds.uuid("farm-001"), "item-004", "DIESEL_S10", BigDecimal("5200")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-005"), DemoIds.uuid("farm-002"), "item-005", "TWO_FOUR_D", BigDecimal("310")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-006"), DemoIds.uuid("farm-002"), "item-006", "OIL_FILTER", BigDecimal("24")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-007"), DemoIds.uuid("farm-003"), "item-007", "CORN_SEED", BigDecimal("1100")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-008"), DemoIds.uuid("farm-003"), "item-008", "MAP", BigDecimal("2400")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-009"), DemoIds.uuid("farm-004"), "item-009", "INSECTICIDE", BigDecimal("180")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-010"), DemoIds.uuid("farm-004"), "item-010", "DIESEL_S10", BigDecimal("3800")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-011"), DemoIds.uuid("farm-005"), "item-011", "KCL", BigDecimal("1600")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-012"), DemoIds.uuid("farm-005"), "item-012", "DRIVE_BELT", BigDecimal("12")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-013"), DemoIds.uuid("farm-006"), "item-013", "COTTON_SEED", BigDecimal("640")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-014"), DemoIds.uuid("farm-006"), "item-014", "PRE_EMERGENT", BigDecimal("220")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-015"), DemoIds.uuid("farm-007"), "item-015", "UREA", BigDecimal("980")),
            ReportInventoryEntity(DemoIds.uuid("rpt-item-016"), DemoIds.uuid("farm-008"), "item-016", "HYDRAULIC_OIL", BigDecimal("450")),
        )
    }

    private fun operationRows(scope: AccessScope, farmId: UUID?): List<ReportOperationEntity> =
        operations.findByFarmIdIn(scope.resolveFarms(farmId)).sortedBy { it.code }

    private fun inventoryRows(scope: AccessScope, farmId: UUID?): List<ReportInventoryEntity> =
        inventory.findByFarmIdIn(scope.resolveFarms(farmId)).sortedBy { it.code }

    private fun subtitle(scope: AccessScope, farmId: UUID?): String {
        val names = scope.resolveFarms(farmId)
            .mapNotNull(DemoCatalog::farmName)
            .ifEmpty { listOf("Fazendas selecionadas") }
        return names.joinToString(" | ")
    }
}

@Service
class ReportingSeed(
    private val svc: ReportingService,
    private val gate: com.precisionfarming.security.DemoSeedGate,
) {
    @Bean
    fun seedReporting() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
