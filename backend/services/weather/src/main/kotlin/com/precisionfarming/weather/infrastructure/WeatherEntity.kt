package com.precisionfarming.weather.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "weather_forecasts")
class WeatherEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "forecast_at") val forecastAt: Instant,
    @Column(name = "temperature_min") val temperatureMin: BigDecimal,
    @Column(name = "temperature_max") val temperatureMax: BigDecimal,
    @Column(name = "rain_mm") val rainMm: BigDecimal,
    @Column(name = "rain_probability") val rainProbability: BigDecimal,
    @Column(name = "wind_kmh") val windKmh: BigDecimal,
    @Column(name = "humidity_pct") val humidityPct: BigDecimal,
    @Column(name = "spraying_window") val sprayingWindow: String,
    val vintage: String,
)

interface WeatherJpaRepository : JpaRepository<WeatherEntity, UUID> {
    fun existsByFarmId(farmId: UUID): Boolean
    fun findByFarmIdOrderByForecastAtAsc(farmId: UUID): List<WeatherEntity>
    fun findFirstByFarmIdAndForecastAtGreaterThanEqualOrderByForecastAtAsc(
        farmId: UUID,
        forecastAt: Instant,
    ): WeatherEntity?
    fun findFirstByFarmIdAndForecastAtLessThanOrderByForecastAtDesc(
        farmId: UUID,
        forecastAt: Instant,
    ): WeatherEntity?
}
