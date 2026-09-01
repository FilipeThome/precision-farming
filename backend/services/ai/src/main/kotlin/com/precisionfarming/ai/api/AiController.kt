package com.precisionfarming.ai.api

import com.precisionfarming.ai.application.AiService
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai")
class AiController(
    private val svc: AiService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/insights")
    fun insights(@RequestParam(required = false) farmId: UUID?) = svc.insights(farmAccess.current(), farmId)

    @GetMapping("/predictions")
    fun predictions(@RequestParam fieldId: UUID) = svc.predictions(farmAccess.current(), fieldId)

    @GetMapping("/machine-risk")
    fun risk(@RequestParam machineId: UUID) = svc.machineRisk(farmAccess.current(), machineId)

    @PostMapping("/recommendations/{id}/feedback")
    fun feedback(@PathVariable id: UUID) = svc.feedback(farmAccess.current(), id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class AiSeedController(private val svc: AiService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "ai").also { svc.seed() }
}
