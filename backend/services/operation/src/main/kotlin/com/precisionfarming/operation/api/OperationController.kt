package com.precisionfarming.operation.api

import com.precisionfarming.operation.application.CreateOperation
import com.precisionfarming.operation.application.OperationService
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class PauseRequest(val reason: String? = null)

@RestController
@RequestMapping("/api/v1/operations")
class OperationController(private val svc: OperationService) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmId)

    @PostMapping
    fun create(@RequestBody body: CreateOperation) = svc.create(body)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = svc.get(id)

    @PostMapping("/{id}/start")
    fun start(@PathVariable id: UUID) = svc.start(id)

    @PostMapping("/{id}/pause")
    fun pause(@PathVariable id: UUID, @RequestBody(required = false) body: PauseRequest?) =
        svc.pause(id, body?.reason)

    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: UUID) = svc.complete(id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class OperationSeedController(private val svc: OperationService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "operation").also { svc.seed() }
}
