package com.precisionfarming.asset.api

import com.precisionfarming.asset.application.AssetService
import com.precisionfarming.asset.application.CreateWorkOrder
import com.precisionfarming.asset.application.UpsertMachine
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/machines")
class AssetController(private val svc: AssetService) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmId)

    @PostMapping
    fun create(@RequestBody body: UpsertMachine) = svc.create(body)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = svc.get(id)

    @PatchMapping("/{id}")
    fun patch(@PathVariable id: UUID, @RequestBody body: UpsertMachine) = svc.patch(id, body)
}

@RestController
@RequestMapping("/api/v1/maintenance/work-orders")
class MaintenanceController(private val svc: AssetService) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.listWorkOrders(farmId)

    @PostMapping
    fun create(@RequestBody body: CreateWorkOrder) = svc.createWorkOrder(body)

    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: UUID) = svc.completeWorkOrder(id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class AssetSeedController(private val svc: AssetService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "asset").also { svc.seed() }
}
