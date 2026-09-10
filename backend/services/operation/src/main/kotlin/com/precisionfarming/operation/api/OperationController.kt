package com.precisionfarming.operation.api

import com.precisionfarming.operation.application.CreateOperation
import com.precisionfarming.operation.application.OperationService
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class PauseRequest(val reason: String? = null)

@RestController
@RequestMapping("/api/v1/operations")
class OperationController(
    private val svc: OperationService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.list(farmAccess.current(), farmId)

    @GetMapping("/machine-summary")
    fun machineSummary(
        @RequestParam machineId: UUID,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ) = svc.machineSummary(
        farmAccess.current(),
        machineId,
        from ?: Instant.now().minus(7, ChronoUnit.DAYS),
        to ?: Instant.now(),
    )

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    fun create(@RequestBody body: CreateOperation) = svc.create(farmAccess.current(), body)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = svc.get(farmAccess.current(), id)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER','OPERATOR')")
    @PostMapping("/{id}/start")
    fun start(@PathVariable id: UUID) = svc.start(farmAccess.current(), id)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER','OPERATOR')")
    @PostMapping("/{id}/pause")
    fun pause(@PathVariable id: UUID, @RequestBody(required = false) body: PauseRequest?) =
        svc.pause(farmAccess.current(), id, body?.reason)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER','OPERATOR')")
    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: UUID) = svc.complete(farmAccess.current(), id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class OperationSeedController(private val svc: OperationService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "operation").also { svc.seed() }
}
