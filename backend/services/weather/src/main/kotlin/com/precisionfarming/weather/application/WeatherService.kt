package com.precisionfarming.weather.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.concurrency.VirtualJobs
import com.precisionfarming.weather.infrastructure.WeatherEntity
import com.precisionfarming.weather.infrastructure.WeatherJpaRepository
import com.precisionfarming.weather.infrastructure.WeatherWindowEntity
import com.precisionfarming.weather.infrastructure.WeatherWindowJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable

interface WeatherProvider {
    fun demoForecast(farmId: UUID): List<WeatherEntity>
}

class DemoWeatherProvider : WeatherProvider {
    override fun demoForecast(farmId: UUID): List<WeatherEntity> {
        val now = Instant.now().truncatedTo(ChronoUnit.DAYS)
        return (-14..7).map { day ->
            val rain = if (day % 3 == 0) BigDecimal("4.8") else BigDecimal("0.2")
            WeatherEntity(
                id = UUID.randomUUID(),
                farmId = farmId,
                forecastAt = now.plus(day.toLong(), ChronoUnit.DAYS),
                temperatureMin = BigDecimal("18"),
                temperatureMax = BigDecimal("29"),
                rainMm = rain,
                rainProbability = if (rain > BigDecimal.ONE) BigDecimal("72") else BigDecimal("20"),
                windKmh = BigDecimal("14"),
                humidityPct = BigDecimal("67"),
                sprayingWindow = if (rain > BigDecimal.ONE) "UNFAVORABLE" else "FAVORABLE",
                vintage = "demo-v1",
            )
        }
    }
}

data class WeatherWindowDto(
    val id: UUID,
    val farmId: UUID,
    val windowType: String,
    val startAt: Instant,
    val endAt: Instant,
    val rating: String,
    val notes: String?,
)

@Service
class WeatherService(
    private val repo: WeatherJpaRepository,
    private val windows: WeatherWindowJpaRepository,
) {
    private val provider: WeatherProvider = DemoWeatherProvider()

    fun current(farmId: UUID): WeatherEntity? {
        ensureForecast(farmId)
        val now = Instant.now()
        return repo.findFirstByFarmIdAndForecastAtGreaterThanEqualOrderByForecastAtAsc(farmId, now)
            ?: repo.findFirstByFarmIdAndForecastAtLessThanOrderByForecastAtDesc(farmId, now)
    }

    fun forecast(farmId: UUID) = repo.findByFarmIdOrderByForecastAtAsc(farmId).ifEmpty {
        provider.demoForecast(farmId).also { repo.saveAll(it) }
    }

    fun listWindows(farmId: UUID?, type: String?) =
        (farmId?.let { windows.findByFarmId(it) } ?: windows.findAll())
            .filter { type == null || it.windowType.equals(type, ignoreCase = true) }
            .map { WeatherWindowDto(it.id, it.farmId, it.windowType, it.startAt, it.endAt, it.rating, it.notes) }

    @Transactional
    fun seed() {
        val keys = listOf("farm-001", "farm-002", "farm-003", "farm-004", "farm-005")
        val missing = keys.map { DemoIds.uuid(it) }.filter { !repo.existsByFarmId(it) }
        if (missing.isNotEmpty()) {
            val rows = VirtualJobs.all(missing.map { farmId -> Callable { provider.demoForecast(farmId) } })
            repo.saveAll(rows.flatten())
        }
        if (!windows.existsById(DemoIds.uuid("wwin-001"))) {
            val now = Instant.now()
            val types = listOf("SPRAYING", "PLANTING", "HARVEST")
            val ratings = listOf("FAVORABLE", "MARGINAL", "UNFAVORABLE")
            windows.saveAll(
                (1..14).map { i ->
                    val farm = keys[(i - 1) % keys.size]
                    val type = types[(i - 1) % types.size]
                    WeatherWindowEntity(
                        DemoIds.uuid("wwin-%03d".format(i)),
                        DemoIds.uuid(farm),
                        type,
                        now.plus((i - 1).toLong(), ChronoUnit.DAYS),
                        now.plus(i.toLong(), ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS),
                        ratings[i % ratings.size],
                        "Janela demo $type #$i",
                    )
                },
            )
        }
    }

    private fun ensureForecast(farmId: UUID) {
        if (!repo.existsByFarmId(farmId)) {
            repo.saveAll(provider.demoForecast(farmId))
        }
    }
}

@Service
class WeatherSeed(
    private val svc: WeatherService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedWeather() = ApplicationRunner { if (seed) svc.seed() }
}
