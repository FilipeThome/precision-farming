package com.precisionfarming.alert.api

import com.precisionfarming.alert.application.AlertService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/alerts")
class AlertController(private val svc: AlertService) {
    @GetMapping fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmId)
    @PostMapping("/{id}/ack") fun ack(@PathVariable id: UUID) = svc.ack(id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class AlertSeedController(private val svc: AlertService) {
    @PostMapping("/reset") fun reset() = mapOf("status" to "seeded", "service" to "alert").also { svc.seed() }
}
