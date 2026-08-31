package com.precisionfarming.reporting.api

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reports")
class ReportingController {
    @GetMapping("/operations.csv")
    fun operations(): ResponseEntity<String> {
        val csv = "id,type,status\nop-001,Plantio,COMPLETED\nop-002,Pulverização,IN_PROGRESS\n"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=operations.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv)
    }

    @GetMapping("/inventory.csv")
    fun inventory(): ResponseEntity<String> {
        val csv = "id,name,quantity\nitem-001,Glifosato,420\n"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv)
    }
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class ReportingSeedController {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "reporting")
}
