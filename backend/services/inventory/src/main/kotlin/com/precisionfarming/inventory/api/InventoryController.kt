package com.precisionfarming.inventory.api

import com.precisionfarming.inventory.application.InventoryService
import com.precisionfarming.inventory.application.MovementCmd
import com.precisionfarming.inventory.application.UpsertItem
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(private val svc: InventoryService) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmId)

    @PostMapping
    fun create(@RequestBody body: UpsertItem) = svc.create(body)

    @PostMapping("/movements")
    fun move(@RequestBody body: MovementCmd) = svc.move(body)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class InventorySeedController(private val svc: InventoryService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "inventory").also { svc.seed() }
}
