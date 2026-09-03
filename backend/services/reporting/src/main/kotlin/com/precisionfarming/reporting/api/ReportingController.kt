package com.precisionfarming.reporting.api

import com.precisionfarming.reporting.application.ReportingService
import com.precisionfarming.security.FarmAccess
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/reports")
class ReportingController(
    private val farmAccess: FarmAccess,
    private val svc: ReportingService,
) {
    @GetMapping
    fun catalog() = svc.reportCatalog()

    @GetMapping("/operations.csv")
    fun operations(@RequestParam(required = false) farmId: UUID?): ResponseEntity<String> {
        val csv = svc.operationsCsv(farmAccess.current(), farmId)
        return csvAttachment("operations.csv", csv)
    }

    @GetMapping("/inventory.csv")
    fun inventory(@RequestParam(required = false) farmId: UUID?): ResponseEntity<String> {
        val csv = svc.inventoryCsv(farmAccess.current(), farmId)
        return csvAttachment("inventory.csv", csv)
    }

    @GetMapping("/operations.pdf")
    fun operationsPdf(@RequestParam(required = false) farmId: UUID?): ResponseEntity<ByteArray> {
        val pdf = svc.operationsPdf(farmAccess.current(), farmId)
        return pdfAttachment("operations.pdf", pdf)
    }

    @GetMapping("/inventory.pdf")
    fun inventoryPdf(@RequestParam(required = false) farmId: UUID?): ResponseEntity<ByteArray> {
        val pdf = svc.inventoryPdf(farmAccess.current(), farmId)
        return pdfAttachment("inventory.pdf", pdf)
    }

    private fun csvAttachment(filename: String, csv: String) = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv)

    private fun pdfAttachment(filename: String, pdf: ByteArray) = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class ReportingSeedController(private val svc: ReportingService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset(): Map<String, String> {
        svc.seed()
        return mapOf("status" to "seeded", "service" to "reporting")
    }
}
