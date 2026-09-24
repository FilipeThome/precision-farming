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

@Entity
@Table(name = "weather_windows")
class WeatherWindowEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "window_type") val windowType: String,
    @Column(name = "start_at") val startAt: Instant,
    @Column(name = "end_at") val endAt: Instant,
    val rating: String,
    val notes: String?,
)

@Entity
@Table(name = "planting_gate_configs")
class PlantingGateEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val municipality: String,
    val crop: String,
    @Column(name = "zarc_start_month") val zarcStartMonth: Int,
    @Column(name = "zarc_start_day") val zarcStartDay: Int,
    @Column(name = "zarc_end_month") val zarcEndMonth: Int,
    @Column(name = "zarc_end_day") val zarcEndDay: Int,
    @Column(name = "void_start_month") val voidStartMonth: Int,
    @Column(name = "void_start_day") val voidStartDay: Int,
    @Column(name = "void_end_month") val voidEndMonth: Int,
    @Column(name = "void_end_day") val voidEndDay: Int,
)

interface WeatherJpaRepository : JpaRepository<WeatherEntity, UUID> {
    fun existsByFarmId(farmId: UUID): Boolean
    fun deleteByFarmId(farmId: UUID): Long
    fun findByFarmIdOrderByForecastAtAsc(farmId: UUID): List<WeatherEntity>
    fun findFirstByFarmIdAndForecastAtGreaterThanEqualOrderByForecastAtAsc(
        farmId: UUID,
        forecastAt: Instant,
    ): WeatherEntity?
    fun findFirstByFarmIdAndForecastAtLessThanOrderByForecastAtDesc(
        farmId: UUID,
        forecastAt: Instant,
    ): WeatherEntity?
    fun findByFarmIdAndForecastAtGreaterThanEqualAndForecastAtLessThanEqualOrderByForecastAtAsc(
        farmId: UUID,
        from: Instant,
        to: Instant,
    ): List<WeatherEntity>
}

interface WeatherWindowJpaRepository : JpaRepository<WeatherWindowEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<WeatherWindowEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<WeatherWindowEntity>
    fun findByFarmIdInAndWindowTypeIgnoreCase(farmIds: Collection<UUID>, windowType: String): List<WeatherWindowEntity>
}

interface PlantingGateJpaRepository : JpaRepository<PlantingGateEntity, UUID> {
    fun findByFarmIdAndCropIgnoreCase(farmId: UUID, crop: String): PlantingGateEntity?
}
