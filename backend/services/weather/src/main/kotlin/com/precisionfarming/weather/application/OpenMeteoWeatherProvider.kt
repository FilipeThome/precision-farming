package com.precisionfarming.weather.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.precisionfarming.common.DemoIds
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Open-Meteo daily forecast. No API key.
 * https://api.open-meteo.com/v1/forecast
 *
 * Weather.com (The Weather Company) agriculture hourly forecast is the licensed
 * equivalent (`api.weather.com/v3/wx/forecast/hourly/agriculture/15day`) and needs
 * a contracted apiKey. This provider is the one the service can call without a secret.
 */
class OpenMeteoWeatherProvider(
    private val fetchDaily: (latitude: Double, longitude: Double) -> String = ::fetchOpenMeteoDaily,
) : WeatherProvider {
    override fun isLive(): Boolean = true

    override fun demoForecast(farmId: UUID): List<WeatherDto> {
        val coord = FARM_COORDINATES[farmId] ?: return emptyList()
        return try {
            parseOpenMeteoDaily(farmId, fetchDaily(coord.latitude, coord.longitude))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private data class LatLon(val latitude: Double, val longitude: Double)

    companion object {
        private val FARM_COORDINATES: Map<UUID, LatLon> = mapOf(
            "farm-001" to LatLon(-19.39, -54.57),
            "farm-002" to LatLon(-17.79, -50.92),
            "farm-003" to LatLon(-12.54, -55.47),
            "farm-004" to LatLon(-13.05, -55.90),
            "farm-005" to LatLon(-22.22, -54.80),
            "farm-006" to LatLon(-13.68, -57.88),
            "farm-007" to LatLon(-16.62, -54.10),
            "farm-008" to LatLon(-18.79, -52.62),
        ).mapKeys { DemoIds.uuid(it.key) }
    }
}

internal fun fetchOpenMeteoDaily(latitude: Double, longitude: Double): String {
    val uri = URI.create(
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum," +
            "precipitation_probability_max,wind_speed_10m_max,relative_humidity_2m_mean" +
            "&timezone=UTC&forecast_days=7&past_days=14",
    )
    val request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(8))
        .header("User-Agent", "precision-farming/weather")
        .GET()
        .build()
    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
        throw IllegalStateException("Open-Meteo HTTP ${response.statusCode()}")
    }
    return response.body()
}

internal fun parseOpenMeteoDaily(farmId: UUID, body: String): List<WeatherDto> {
    val daily = ObjectMapper().readTree(body).path("daily")
    val dates = daily.path("time")
    if (!dates.isArray || dates.isEmpty) return emptyList()
    return dates.mapIndexed { index, node ->
        val date = LocalDate.parse(node.asText())
        val rain = decimalAt(daily, "precipitation_sum", index)
        val wind = decimalAt(daily, "wind_speed_10m_max", index)
        val rainProbability = decimalAt(daily, "precipitation_probability_max", index)
        WeatherDto(
            id = DemoIds.uuid("wx-om-$farmId-$date"),
            farmId = farmId,
            forecastAt = date.atStartOfDay().toInstant(ZoneOffset.UTC),
            temperatureMin = decimalAt(daily, "temperature_2m_min", index),
            temperatureMax = decimalAt(daily, "temperature_2m_max", index),
            rainMm = rain,
            rainProbability = rainProbability,
            windKmh = wind,
            humidityPct = decimalAt(daily, "relative_humidity_2m_mean", index),
            sprayingWindow = if (rain > BigDecimal("2") || wind > BigDecimal("20")) "UNFAVORABLE" else "FAVORABLE",
            vintage = "open-meteo",
        )
    }
}

@Configuration
class WeatherProviderConfig {
    @Bean
    fun weatherProvider(@Value("\${weather.provider:demo}") mode: String): WeatherProvider =
        if (mode.equals("open-meteo", ignoreCase = true)) OpenMeteoWeatherProvider()
        else DemoWeatherProvider()
}

private fun decimalAt(daily: com.fasterxml.jackson.databind.JsonNode, field: String, index: Int): BigDecimal {
    val node = daily.path(field).path(index)
    if (node.isMissingNode || node.isNull) return BigDecimal.ZERO
    return BigDecimal.valueOf(node.asDouble()).setScale(1, RoundingMode.HALF_UP)
}
