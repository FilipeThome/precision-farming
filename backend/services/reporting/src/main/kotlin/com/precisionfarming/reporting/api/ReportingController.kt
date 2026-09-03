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

    private fun csvAttachment(filename: String, csv: String) = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv)
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
