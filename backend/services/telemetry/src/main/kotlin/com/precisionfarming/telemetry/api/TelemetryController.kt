package com.precisionfarming.telemetry.api

import com.precisionfarming.security.FarmAccess
import com.precisionfarming.telemetry.application.TelemetryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/api/v1/machines")
class TelemetryController(
    private val svc: TelemetryService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/{id}/telemetry")
    fun telemetry(
        @PathVariable id: UUID,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ) = svc.history(
        farmAccess.current(),
        id,
        from ?: Instant.now().minus(2, ChronoUnit.DAYS),
        to ?: Instant.now(),
    )

    @GetMapping("/{id}/track")
    fun track(
        @PathVariable id: UUID,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ) = svc.track(
        farmAccess.current(),
        id,
        from ?: Instant.now().minus(1, ChronoUnit.DAYS),
        to ?: Instant.now(),
    )

    @GetMapping("/{id}/metrics")
    fun metrics(
        @PathVariable id: UUID,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ) = svc.metrics(
        farmAccess.current(),
        id,
        from ?: Instant.now().minus(7, ChronoUnit.DAYS),
        to ?: Instant.now(),
    )
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class TelemetrySeedController(private val svc: TelemetryService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "telemetry").also { svc.seed() }
}
