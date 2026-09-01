package com.precisionfarming.weather.api

import com.precisionfarming.common.DemoIds
import com.precisionfarming.weather.application.WeatherService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/weather")
class WeatherController(private val svc: WeatherService) {
    @GetMapping("/current")
    fun current(@RequestParam(required = false) farmId: UUID?) =
        svc.current(farmId ?: DemoIds.uuid("farm-001"))

    @GetMapping("/forecast")
    fun forecast(@RequestParam(required = false) farmId: UUID?) =
        svc.forecast(farmId ?: DemoIds.uuid("farm-001"))

    @GetMapping("/windows")
    fun windows(
        @RequestParam(required = false) farmId: UUID?,
        @RequestParam(required = false) type: String?,
    ) = svc.listWindows(farmId, type)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class WeatherSeedController(private val svc: WeatherService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "weather").also { svc.seed() }
}
