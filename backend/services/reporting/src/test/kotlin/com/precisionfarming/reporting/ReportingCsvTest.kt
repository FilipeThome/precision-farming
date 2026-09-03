package com.precisionfarming.reporting

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.reporting.application.ReportingService
import com.precisionfarming.reporting.infrastructure.ReportInventoryEntity
import com.precisionfarming.reporting.infrastructure.ReportInventoryJpaRepository
import com.precisionfarming.reporting.infrastructure.ReportOperationEntity
import com.precisionfarming.reporting.infrastructure.ReportOperationJpaRepository
import com.precisionfarming.security.AccessScope
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ReportingCsvTest {
    private val operations = mockk<ReportOperationJpaRepository>()
    private val inventory = mockk<ReportInventoryJpaRepository>()
    private val svc = ReportingService(operations, inventory)
    private val farm1 = DemoIds.uuid("farm-001")
    private val farm2 = DemoIds.uuid("farm-002")
    private val tenant = DemoIds.uuid("tenant-demo")

    @Test
    fun operationsCsvUsesDatabaseRows() {
        every { operations.findByFarmIdIn(setOf(farm1)) } returns listOf(
            ReportOperationEntity(DemoIds.uuid("rpt-op-001"), farm1, "op-001", "PLANTING", "COMPLETED"),
        )
        val csv = svc.operationsCsv(AccessScope(tenant, setOf(farm1), "OPERATOR"), farm1)
        assertTrue(csv.contains("id,type,status"))
        assertTrue(csv.contains("op-001,PLANTING,COMPLETED"))
    }

    @Test
    fun inventoryCsvUsesDatabaseRows() {
        every { inventory.findByFarmIdIn(setOf(farm1)) } returns listOf(
            ReportInventoryEntity(DemoIds.uuid("rpt-item-001"), farm1, "item-001", "GLYPHOSATE", BigDecimal("420")),
        )
        val csv = svc.inventoryCsv(AccessScope(tenant, setOf(farm1), "OPERATOR"), farm1)
        assertTrue(csv.contains("item-001,GLYPHOSATE,420"))
    }

    @Test
    fun reportCatalogListsAvailablePdfReports() {
        val catalog = svc.reportCatalog()

        assertEquals(listOf("operations", "inventory"), catalog.map { it.kind })
        assertTrue(catalog.all { it.format == "PDF" })
        assertEquals("/api/v1/reports/operations.pdf", catalog.first().path)
    }

    @Test
    fun operationsPdfContainsHeaderAndRows() {
        every { operations.findByFarmIdIn(setOf(farm1)) } returns listOf(
            ReportOperationEntity(DemoIds.uuid("rpt-op-001"), farm1, "op-001", "PLANTING", "COMPLETED"),
        )

        val pdf = svc.operationsPdf(AccessScope(tenant, setOf(farm1), "OPERATOR"), farm1)
        val text = pdf.toString(Charsets.ISO_8859_1)

        assertTrue(text.startsWith("%PDF"))
        assertTrue(text.contains("Relatorio de Operacoes"))
        assertTrue(text.contains("op-001"))
        assertTrue(text.contains("Precision Farming"))
        assertTrue(text.contains("xref"))
        assertTrue(text.contains("trailer << /Size "))
    }

    @Test
    fun inventoryPdfContainsHeaderAndRows() {
        every { inventory.findByFarmIdIn(setOf(farm1)) } returns listOf(
            ReportInventoryEntity(DemoIds.uuid("rpt-item-001"), farm1, "item-001", "GLYPHOSATE", BigDecimal("420")),
        )

        val pdf = svc.inventoryPdf(AccessScope(tenant, setOf(farm1), "OPERATOR"), farm1)
        val text = pdf.toString(Charsets.ISO_8859_1)

        assertTrue(text.startsWith("%PDF"))
        assertTrue(text.contains("Relatorio de Estoque"))
        assertTrue(text.contains("item-001"))
        assertTrue(text.contains("GLYPHOSATE"))
        assertTrue(text.contains("startxref"))
        assertTrue(text.contains("%%EOF"))
    }

    @Test
    fun seedMatchesOperationAndInventoryCatalog() {
        val savedOps = mutableListOf<ReportOperationEntity>()
        val savedItems = mutableListOf<ReportInventoryEntity>()
        every { operations.saveAll(any<Iterable<ReportOperationEntity>>()) } answers {
            val rows = firstArg<Iterable<ReportOperationEntity>>().toList()
            savedOps += rows
            rows
        }
        every { inventory.saveAll(any<Iterable<ReportInventoryEntity>>()) } answers {
            val rows = firstArg<Iterable<ReportInventoryEntity>>().toList()
            savedItems += rows
            rows
        }

        svc.seed()

        assertEquals(20, savedOps.size)
        assertEquals(16, savedItems.size)
        assertEquals((1..20).map { "op-%03d".format(it) }.toSet(), savedOps.map { it.code }.toSet())
        assertEquals((1..16).map { "item-%03d".format(it) }.toSet(), savedItems.map { it.code }.toSet())
        assertEquals("HYDRAULIC_OIL", savedItems.single { it.code == "item-016" }.name)
        assertEquals(DemoIds.uuid("farm-008"), savedItems.single { it.code == "item-016" }.farmId)
    }

    @Test
    fun csvEscapesCommasQuotesAndFormulas() {
        assertEquals("plain", ReportingService.csvCell("plain"))
        assertEquals("\"a,b\"", ReportingService.csvCell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", ReportingService.csvCell("say \"hi\""))
        assertEquals("\"'=CMD()\"", ReportingService.csvCell("=CMD()"))
        assertTrue(ReportingService.csvCell(" =CMD()").contains("'="))
        every { inventory.findByFarmIdIn(setOf(farm1)) } returns listOf(
            ReportInventoryEntity(UUID.randomUUID(), farm1, "item-x", "GLYPHOSATE, \"extra\"", BigDecimal("1")),
        )
        val csv = svc.inventoryCsv(AccessScope(tenant, setOf(farm1), "OPERATOR"), farm1)
        assertTrue(csv.contains("\"GLYPHOSATE, \"\"extra\"\"\""))
    }

    @Test
    fun outOfScopeFarmIdIsForbidden() {
        assertThrows(ForbiddenException::class.java) {
            svc.operationsCsv(AccessScope(tenant, setOf(farm1), "OPERATOR"), farm2)
        }
    }

    @Test
    fun outOfScopeFarmIdIsForbiddenForPdf() {
        assertThrows(ForbiddenException::class.java) {
            svc.operationsPdf(AccessScope(tenant, setOf(farm1), "OPERATOR"), farm2)
        }
    }

    @Test
    fun seededOperationsAreNotAStaleSubset() {
        assertFalse(ReportingService.demoOperations().size < 20)
        assertEquals(20, ReportingService.demoOperations().distinctBy { it.code }.size)
        assertEquals(16, ReportingService.demoInventory().distinctBy { it.code }.size)
    }
}
