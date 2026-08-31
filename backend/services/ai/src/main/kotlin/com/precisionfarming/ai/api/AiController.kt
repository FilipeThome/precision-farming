package com.precisionfarming.ai.api

import com.precisionfarming.ai.application.AiService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai")
class AiController(private val svc: AiService) {
    @GetMapping("/insights") fun insights(@RequestParam(required = false) farmId: UUID?) = svc.insights(farmId)
    @GetMapping("/predictions") fun predictions(@RequestParam fieldId: UUID) = svc.predictions(fieldId)
    @GetMapping("/machine-risk") fun risk(@RequestParam machineId: UUID) = svc.machineRisk(machineId)
    @PostMapping("/recommendations/{id}/feedback") fun feedback(@PathVariable id: UUID) = svc.feedback(id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class AiSeedController(private val svc: AiService) {
    @PostMapping("/reset") fun reset() = mapOf("status" to "seeded", "service" to "ai").also { svc.seed() }
}
