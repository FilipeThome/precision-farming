package com.precisionfarming.irrigation.api

import com.precisionfarming.irrigation.application.IrrigationService
import com.precisionfarming.irrigation.application.SimulateRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/irrigation")
class IrrigationController(private val svc: IrrigationService) {
    @GetMapping("/assets")
    fun assets(@RequestParam(required = false) farmId: UUID?) = svc.listAssets(farmId)

    @GetMapping("/recommendations")
    fun recommendations(@RequestParam(required = false) farmId: UUID?) = svc.listRecommendations(farmId)

    @PostMapping("/simulate")
    fun simulate(@RequestBody body: SimulateRequest) = svc.simulate(body)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class IrrigationSeedController(private val svc: IrrigationService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "irrigation").also { svc.seed() }
}
