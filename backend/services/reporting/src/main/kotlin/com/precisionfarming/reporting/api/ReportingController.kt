package com.precisionfarming.reporting.api

import com.precisionfarming.security.FarmAccess
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.access.prepost.PreAuthorize
import java.util.UUID

@RestController
@RequestMapping("/api/v1/reports")
class ReportingController(private val farmAccess: FarmAccess) {
    @GetMapping("/operations.csv")
    fun operations(@RequestParam(required = false) farmId: UUID?): ResponseEntity<String> {
        val scope = farmAccess.current()
        farmId?.let { scope.requireFarm(it) }
        val csv = "id,type,status\nop-001,Plantio,COMPLETED\nop-002,Pulverização,IN_PROGRESS\n"
        return csvAttachment("operations.csv", csv)
    }

    @GetMapping("/inventory.csv")
    fun inventory(@RequestParam(required = false) farmId: UUID?): ResponseEntity<String> {
        val scope = farmAccess.current()
        farmId?.let { scope.requireFarm(it) }
        val csv = "id,name,quantity\nitem-001,Glifosato,420\n"
        return csvAttachment("inventory.csv", csv)
    }

    private fun csvAttachment(filename: String, csv: String) = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class ReportingSeedController {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "reporting")
}
