package com.precisionfarming.harvest.api

import com.precisionfarming.harvest.application.CreateHarvestPlan
import com.precisionfarming.harvest.application.DispatchRequest
import com.precisionfarming.harvest.application.HarvestService
import com.precisionfarming.harvest.application.UpsertStorageUnit
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class HarvestController(
    private val svc: HarvestService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/harvest/plans")
    fun plans(@RequestParam(required = false) farmId: UUID?) = svc.listPlans(farmAccess.current(), farmId)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PostMapping("/harvest/plans")
    fun createPlan(@RequestBody body: CreateHarvestPlan) = svc.createPlan(farmAccess.current(), body)

    @GetMapping("/harvest/yield")
    fun yield(@RequestParam(required = false) farmId: UUID?) = svc.listYield(farmAccess.current(), farmId)

    @GetMapping("/logistics/loads")
    fun loads(@RequestParam(required = false) farmId: UUID?) = svc.listLoads(farmAccess.current(), farmId)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PostMapping("/logistics/dispatch")
    fun dispatch(@RequestBody body: DispatchRequest) = svc.dispatch(farmAccess.current(), body)

    @GetMapping("/storage/units")
    fun units(@RequestParam(required = false) farmId: UUID?) = svc.listUnits(farmAccess.current(), farmId)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PostMapping("/storage/units")
    fun createUnit(@RequestBody body: UpsertStorageUnit) = svc.createUnit(farmAccess.current(), body)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PatchMapping("/storage/units/{id}")
    fun patchUnit(@PathVariable id: UUID, @RequestBody body: UpsertStorageUnit) =
        svc.patchUnit(farmAccess.current(), id, body)

    @GetMapping("/storage/lots")
    fun lots(@RequestParam(required = false) farmId: UUID?) = svc.listLots(farmAccess.current(), farmId)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class HarvestSeedController(private val svc: HarvestService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "harvest").also { svc.seed() }
}
