package com.precisionfarming.integration.api

import com.precisionfarming.integration.application.IntegrationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/integrations")
class IntegrationController(private val svc: IntegrationService) {
    @GetMapping
    fun list() = svc.list()
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class IntegrationSeedController(private val svc: IntegrationService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset(): Map<String, String> {
        svc.seed()
        return mapOf("status" to "seeded", "service" to "integration")
    }
}
