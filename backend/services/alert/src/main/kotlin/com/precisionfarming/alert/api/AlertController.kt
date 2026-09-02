package com.precisionfarming.alert.api

import com.precisionfarming.alert.application.AlertService
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/alerts")
class AlertController(
    private val svc: AlertService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmAccess.current(), farmId)

    @PostMapping("/{id}/ack")
    fun ack(@PathVariable id: UUID) = svc.ack(farmAccess.current(), id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class AlertSeedController(private val svc: AlertService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "alert").also { svc.seed() }
}
