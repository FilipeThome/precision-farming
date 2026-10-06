package com.precisionfarming.weather.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.weather.domain.MapaZarcLookup
import com.precisionfarming.weather.domain.MonthDayWindow
import com.precisionfarming.weather.domain.frostRisk
import com.precisionfarming.weather.domain.plantingGateDecision
import com.precisionfarming.weather.domain.waterDeficitMm
import com.precisionfarming.weather.infrastructure.PlantingGateEntity
import com.precisionfarming.weather.infrastructure.PlantingGateJpaRepository
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
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

interface WeatherProvider {
    fun demoForecast(farmId: UUID): List<WeatherDto>
    fun isLive(): Boolean = false
}

data class WeatherDto(
    val id: UUID,
    val farmId: UUID,
    val forecastAt: Instant,
    val temperatureMin: BigDecimal,
    val temperatureMax: BigDecimal,
    val rainMm: BigDecimal,
    val rainProbability: BigDecimal,
    val windKmh: BigDecimal,
    val humidityPct: BigDecimal,
    val sprayingWindow: String,
    val vintage: String,
)

class DemoWeatherProvider : WeatherProvider {
    override fun demoForecast(farmId: UUID): List<WeatherDto> {
        val now = Instant.now().truncatedTo(ChronoUnit.DAYS)
        return (-14..7).map { day ->
            val rain = if (day % 3 == 0) BigDecimal("4.8") else BigDecimal("0.2")
            WeatherDto(
                id = DemoIds.uuid("wx-${farmId}-$day"),
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

data class PlantingGateDto(
    val farmId: UUID,
    val municipality: String,
    val crop: String,
    val date: LocalDate,
    val decision: String,
    val reason: String?,
    val simulation: Boolean = true,
)

data class ParametricDayDto(
    val date: LocalDate,
    val rainMm: BigDecimal,
    val waterDeficitMm: BigDecimal,
    val frostRisk: Boolean,
)

data class ParametricIndexDto(
    val farmId: UUID,
    val simulation: Boolean = true,
    val days: List<ParametricDayDto>,
)

@Service
class WeatherService(
    private val repo: WeatherJpaRepository,
    private val windows: WeatherWindowJpaRepository,
    private val gates: PlantingGateJpaRepository,
    private val provider: WeatherProvider = DemoWeatherProvider(),
    private val mapa: MapaZarcLookup = MapaZarcLookup { _, _ -> null },
) {

    fun current(scope: AccessScope, farmId: UUID): WeatherDto? {
        scope.requireFarm(farmId)
        if (refreshLive(farmId) == null) ensureForecast(farmId)
        val now = Instant.now()
        val row = repo.findFirstByFarmIdAndForecastAtGreaterThanEqualOrderByForecastAtAsc(farmId, now)
            ?: repo.findFirstByFarmIdAndForecastAtLessThanOrderByForecastAtDesc(farmId, now)
        return row?.toDto()
    }

    fun forecast(scope: AccessScope, farmId: UUID): List<WeatherDto> {
        scope.requireFarm(farmId)
        refreshLive(farmId)?.let { return it.map { row -> row.toDto() } }
        val stored = repo.findByFarmIdOrderByForecastAtAsc(farmId)
        if (stored.isNotEmpty()) return stored.map { it.toDto() }
        val generated = provider.demoForecast(farmId)
        repo.saveAll(generated.map { it.toEntity() })
        return generated
    }

    fun listWindows(scope: AccessScope, farmId: UUID?, type: String?): List<WeatherWindowDto> {
        val farms = scope.resolveFarms(farmId)
        val rows = if (type.isNullOrBlank()) {
            windows.findByFarmIdIn(farms)
        } else {
            windows.findByFarmIdInAndWindowTypeIgnoreCase(farms, type)
        }
        return rows.map { WeatherWindowDto(it.id, it.farmId, it.windowType, it.startAt, it.endAt, it.rating, it.notes) }
    }

    fun plantingGate(scope: AccessScope, farmId: UUID, date: LocalDate, crop: String): PlantingGateDto {
        scope.requireFarmRead(farmId, "WEATHER_GATE_NOT_FOUND", "Planting gate not configured")
        val cfg = gates.findByFarmIdAndCropIgnoreCase(farmId, crop)
            ?: throw NotFoundException("WEATHER_GATE_NOT_FOUND", "Planting gate not configured")
        val seededZarc = MonthDayWindow(
            MonthDay.of(cfg.zarcStartMonth, cfg.zarcStartDay),
            MonthDay.of(cfg.zarcEndMonth, cfg.zarcEndDay),
        )
        // Sanitary void ALWAYS from the seeded gate / portaria window.
        val sanitary = MonthDayWindow(
            MonthDay.of(cfg.voidStartMonth, cfg.voidStartDay),
            MonthDay.of(cfg.voidEndMonth, cfg.voidEndDay),
        )
        val liveZarc = mapa.window(cfg.municipality, cfg.crop)
        val zarc = liveZarc ?: seededZarc
        val result = plantingGateDecision(date, zarc, sanitary)
        return PlantingGateDto(
            farmId = farmId,
            municipality = cfg.municipality,
            crop = cfg.crop,
            date = date,
            decision = result.decision.name,
            reason = result.reason?.name,
            simulation = liveZarc == null,
        )
    }

    fun parametricIndex(
        scope: AccessScope,
        farmId: UUID,
        from: LocalDate?,
        to: LocalDate?,
    ): ParametricIndexDto {
        scope.requireFarm(farmId)
        if (refreshLive(farmId) == null) ensureForecast(farmId)
        val end = to ?: LocalDate.now(ZoneOffset.UTC)
        val start = from ?: end.minusDays(13)
        val fromInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC)
        val toInstant = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1)
        val rows = repo.findByFarmIdAndForecastAtGreaterThanEqualAndForecastAtLessThanEqualOrderByForecastAtAsc(
            farmId, fromInstant, toInstant,
        )
        val days = rows.map { row ->
            val date = LocalDate.ofInstant(row.forecastAt, ZoneOffset.UTC)
            ParametricDayDto(
                date = date,
                rainMm = row.rainMm,
                waterDeficitMm = waterDeficitMm(row.rainMm),
                frostRisk = frostRisk(row.temperatureMin),
            )
        }
        val live = rows.isNotEmpty() && rows.all { it.vintage == "open-meteo" }
        return ParametricIndexDto(farmId = farmId, simulation = !live, days = days)
    }

    @Transactional
    fun seed() {
        val keys = (1..8).map { "farm-%03d".format(it) }
        val farmIds = keys.map { DemoIds.uuid(it) }
        farmIds.forEach { farmId ->
            if (!repo.existsByFarmId(farmId)) {
                repo.saveAll(provider.demoForecast(farmId).map { it.toEntity() })
            }
        }
        val now = Instant.now()
        val types = listOf("SPRAYING", "PLANTING", "HARVEST")
        val ratings = listOf("FAVORABLE", "MARGINAL", "UNFAVORABLE")
        val windowRows = (1..16).map { i ->
            val farm = keys[(i - 1) % keys.size]
            val type = types[(i - 1) % types.size]
            WeatherWindowEntity(
                DemoIds.uuid("wwin-%03d".format(i)),
                DemoIds.uuid(farm),
                type,
                now.plus((i - 1).toLong(), ChronoUnit.DAYS),
                now.plus(i.toLong(), ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS),
                ratings[i % ratings.size],
                "DEMO_WINDOW $type #$i",
            )
        }
        val existingWindows = windows.findAllById(windowRows.map { it.id }).map { it.id }.toSet()
        windows.saveAll(windowRows.filter { it.id !in existingWindows })

        // farm-001 → São Gabriel do Oeste - MS only; other farms stay unconfigured (404)
        val gateId = DemoIds.uuid("pgate-001")
        if (!gates.existsById(gateId)) {
            gates.save(
                PlantingGateEntity(
                    id = gateId,
                    farmId = DemoIds.uuid("farm-001"),
                    municipality = "São Gabriel do Oeste - MS",
                    crop = "SOY",
                    zarcStartMonth = 10, zarcStartDay = 1,
                    zarcEndMonth = 12, zarcEndDay = 20,
                    voidStartMonth = 6, voidStartDay = 15,
                    voidEndMonth = 9, voidEndDay = 15,
                ),
            )
        }
    }

    private fun ensureForecast(farmId: UUID) {
        if (!repo.existsByFarmId(farmId)) {
            repo.saveAll(provider.demoForecast(farmId).map { it.toEntity() })
        }
    }

    /** Writes the live series first. Stale rows are removed only after that save succeeds. */
    private fun refreshLive(farmId: UUID): List<WeatherEntity>? {
        if (!provider.isLive()) return null
        val live = provider.demoForecast(farmId)
        if (live.isEmpty()) return null
        val incoming = live.map { it.toEntity() }
        repo.saveAll(incoming)
        val keep = incoming.map { it.id }.toSet()
        val stale = repo.findByFarmIdOrderByForecastAtAsc(farmId).filter { it.id !in keep }
        if (stale.isNotEmpty()) repo.deleteAll(stale)
        return repo.findByFarmIdOrderByForecastAtAsc(farmId)
    }

    private fun WeatherDto.toEntity() = WeatherEntity(
        id, farmId, forecastAt, temperatureMin, temperatureMax, rainMm, rainProbability, windKmh, humidityPct, sprayingWindow, vintage,
    )

    private fun WeatherEntity.toDto() = WeatherDto(
        id, farmId, forecastAt, temperatureMin, temperatureMax, rainMm, rainProbability, windKmh, humidityPct, sprayingWindow, vintage,
    )
}

@Service
class WeatherSeed(
    private val svc: WeatherService,
    private val gate: com.precisionfarming.security.DemoSeedGate,
) {
    @Bean
    fun seedWeather() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
