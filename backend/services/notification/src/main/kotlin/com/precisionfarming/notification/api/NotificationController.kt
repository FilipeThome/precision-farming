package com.precisionfarming.notification.api

import com.precisionfarming.notification.application.NotificationService
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.prepost.PreAuthorize

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(private val svc: NotificationService) {
    @GetMapping fun list() = svc.list()
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class NotificationSeedController(private val svc: NotificationService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset") fun reset() = mapOf("status" to "seeded", "service" to "notification").also { svc.seed() }
}
