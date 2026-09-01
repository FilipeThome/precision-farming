package com.precisionfarming.harvest.api

import com.precisionfarming.harvest.application.CreateHarvestPlan
import com.precisionfarming.harvest.application.DispatchRequest
import com.precisionfarming.harvest.application.HarvestService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class HarvestController(private val svc: HarvestService) {
    @GetMapping("/harvest/plans")
    fun plans(@RequestParam(required = false) farmId: UUID?) = svc.listPlans(farmId)

    @PostMapping("/harvest/plans")
    fun createPlan(@RequestBody body: CreateHarvestPlan) = svc.createPlan(body)

    @GetMapping("/harvest/yield")
    fun yield(@RequestParam(required = false) farmId: UUID?) = svc.listYield(farmId)

    @GetMapping("/logistics/loads")
    fun loads(@RequestParam(required = false) farmId: UUID?) = svc.listLoads(farmId)

    @PostMapping("/logistics/dispatch")
    fun dispatch(@RequestBody body: DispatchRequest) = svc.dispatch(body)

    @GetMapping("/storage/units")
    fun units(@RequestParam(required = false) farmId: UUID?) = svc.listUnits(farmId)

    @GetMapping("/storage/lots")
    fun lots(@RequestParam(required = false) farmId: UUID?) = svc.listLots(farmId)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class HarvestSeedController(private val svc: HarvestService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "harvest").also { svc.seed() }
}
