package com.precisionfarming.compliance.api

import com.precisionfarming.compliance.application.ComplianceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ComplianceController(private val svc: ComplianceService) {
    @GetMapping("/traceability")
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.listTraceability(farmId)

    @GetMapping("/traceability/{id}")
    fun get(@PathVariable id: UUID) = svc.getTraceability(id)

    @GetMapping("/esg")
    fun esg(@RequestParam(required = false) farmId: UUID?) = svc.listEsg(farmId)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class ComplianceSeedController(private val svc: ComplianceService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "compliance").also { svc.seed() }
}
