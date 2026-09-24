package com.precisionfarming.weather.api

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.FarmAccess
import com.precisionfarming.weather.application.WeatherService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/weather")
class WeatherController(
    private val svc: WeatherService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/current")
    fun current(@RequestParam(required = false) farmId: UUID?) =
        svc.current(farmAccess.current(), farmId ?: DemoIds.uuid("farm-001"))

    @GetMapping("/forecast")
    fun forecast(@RequestParam(required = false) farmId: UUID?) =
        svc.forecast(farmAccess.current(), farmId ?: DemoIds.uuid("farm-001"))

    @GetMapping("/windows")
    fun windows(
        @RequestParam(required = false) farmId: UUID?,
        @RequestParam(required = false) type: String?,
    ) = svc.listWindows(farmAccess.current(), farmId, type)

    @GetMapping("/planting-gate")
    fun plantingGate(
        @RequestParam farmId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam crop: String,
    ) = svc.plantingGate(farmAccess.current(), farmId, date, crop)

    @GetMapping("/parametric-index")
    fun parametricIndex(
        @RequestParam farmId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ) = svc.parametricIndex(farmAccess.current(), farmId, from, to)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class WeatherSeedController(private val svc: WeatherService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "weather").also { svc.seed() }
}
