package com.precisionfarming.asset.api

import com.precisionfarming.asset.application.AssetService
import com.precisionfarming.asset.application.CreateWorkOrder
import com.precisionfarming.asset.application.UpsertMachine
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/machines")
class AssetController(
    private val svc: AssetService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmAccess.current(), farmId)

    @PostMapping
    fun create(@RequestBody body: UpsertMachine) = svc.create(farmAccess.current(), body)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = svc.get(farmAccess.current(), id)

    @PatchMapping("/{id}")
    fun patch(@PathVariable id: UUID, @RequestBody body: UpsertMachine) =
        svc.patch(farmAccess.current(), id, body)
}

@RestController
@RequestMapping("/api/v1/maintenance/work-orders")
class MaintenanceController(
    private val svc: AssetService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.listWorkOrders(farmAccess.current(), farmId)

    @PostMapping
    fun create(@RequestBody body: CreateWorkOrder) = svc.createWorkOrder(farmAccess.current(), body)

    @PreAuthorize("hasAnyRole('ADMIN','MAINTENANCE','FARM_MANAGER')")
    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: UUID) = svc.completeWorkOrder(farmAccess.current(), id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class AssetSeedController(private val svc: AssetService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "asset").also { svc.seed() }
}
