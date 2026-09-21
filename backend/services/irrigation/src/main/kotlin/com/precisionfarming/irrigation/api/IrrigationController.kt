package com.precisionfarming.irrigation.api

import com.precisionfarming.irrigation.application.IrrigationService
import com.precisionfarming.irrigation.application.SimulateRequest
import com.precisionfarming.irrigation.application.UpsertIrrigationAsset
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
@RequestMapping("/api/v1/irrigation")
class IrrigationController(
    private val svc: IrrigationService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/assets")
    fun assets(@RequestParam(required = false) farmId: UUID?) = svc.listAssets(farmAccess.current(), farmId)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PostMapping("/assets")
    fun createAsset(@RequestBody body: UpsertIrrigationAsset) = svc.createAsset(farmAccess.current(), body)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PatchMapping("/assets/{id}")
    fun patchAsset(@PathVariable id: UUID, @RequestBody body: UpsertIrrigationAsset) =
        svc.patchAsset(farmAccess.current(), id, body)

    @GetMapping("/recommendations")
    fun recommendations(@RequestParam(required = false) farmId: UUID?) =
        svc.listRecommendations(farmAccess.current(), farmId)

    @PostMapping("/simulate")
    fun simulate(@RequestBody body: SimulateRequest) = svc.simulate(farmAccess.current(), body)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class IrrigationSeedController(private val svc: IrrigationService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "irrigation").also { svc.seed() }
}
