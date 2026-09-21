package com.precisionfarming.inventory.api

import com.precisionfarming.inventory.application.InventoryService
import com.precisionfarming.inventory.application.MovementCmd
import com.precisionfarming.inventory.application.PatchItem
import com.precisionfarming.inventory.application.UpsertItem
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val svc: InventoryService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmAccess.current(), farmId)

    @GetMapping("/{itemId}/movements")
    fun movements(@PathVariable itemId: UUID) = svc.listMovements(farmAccess.current(), itemId)

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    fun create(@RequestBody body: UpsertItem) = svc.create(farmAccess.current(), body)

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    fun patch(@PathVariable id: UUID, @RequestBody body: PatchItem) = svc.patch(farmAccess.current(), id, body)

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER','SERVICE')")
    fun move(@RequestBody body: MovementCmd) = svc.move(farmAccess.current(), body)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class InventorySeedController(private val svc: InventoryService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "inventory").also { svc.seed() }
}
