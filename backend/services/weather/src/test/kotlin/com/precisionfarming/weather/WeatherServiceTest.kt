package com.precisionfarming.weather

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.weather.application.WeatherDto
import com.precisionfarming.weather.application.WeatherProvider
import com.precisionfarming.weather.application.WeatherService
import com.precisionfarming.weather.infrastructure.PlantingGateJpaRepository
import com.precisionfarming.weather.infrastructure.WeatherEntity
import com.precisionfarming.weather.infrastructure.WeatherJpaRepository
import com.precisionfarming.weather.infrastructure.WeatherWindowJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class WeatherServiceTest {
    private val repo = mockk<WeatherJpaRepository>()
    private val windows = mockk<WeatherWindowJpaRepository>()
    private val gates = mockk<PlantingGateJpaRepository>()
    private val svc = WeatherService(repo, windows, gates)

    @Test
    fun parametricIndexSetsSimulationTrue() {
        val farmId = DemoIds.uuid("farm-001")
        every { repo.existsByFarmId(farmId) } returns true
        val day = LocalDate.of(2026, 9, 20)
        val at = day.atStartOfDay().toInstant(ZoneOffset.UTC)
        every {
            repo.findByFarmIdAndForecastAtGreaterThanEqualAndForecastAtLessThanEqualOrderByForecastAtAsc(
                farmId, any(), any(),
            )
        } returns listOf(
            WeatherEntity(
                UUID.randomUUID(), farmId, at,
                BigDecimal("18"), BigDecimal("29"), BigDecimal("0.2"),
                BigDecimal("20"), BigDecimal("14"), BigDecimal("67"), "FAVORABLE", "demo-v1",
            ),
        )
        val dto = svc.parametricIndex(
            AccessScope(DemoTenant.ID, setOf(farmId), "OPERATOR"),
            farmId,
            day,
            day,
        )
        assertTrue(dto.simulation)
        assertEquals(1, dto.days.size)
        assertEquals(0, BigDecimal("4.80").compareTo(dto.days[0].waterDeficitMm))
    }

    @Test
    fun liveForecastKeepsStoredRowsWhenSaveFails() {
        val farmId = DemoIds.uuid("farm-001")
        val freshId = UUID.randomUUID()
        val at = Instant.parse("2026-09-24T00:00:00Z")
        val provider = object : WeatherProvider {
            override fun isLive() = true
            override fun demoForecast(farmId: UUID) = listOf(
                WeatherDto(
                    freshId, farmId, at,
                    BigDecimal("18"), BigDecimal("30"), BigDecimal("1.0"),
                    BigDecimal("10"), BigDecimal("8"), BigDecimal("60"), "FAVORABLE", "open-meteo",
                ),
            )
        }
        val live = WeatherService(repo, windows, gates, provider)
        val stored = WeatherEntity(
            UUID.randomUUID(), farmId, at,
            BigDecimal("18"), BigDecimal("29"), BigDecimal("0.2"),
            BigDecimal("20"), BigDecimal("14"), BigDecimal("67"), "FAVORABLE", "demo-v1",
        )
        every { repo.saveAll(any<List<WeatherEntity>>()) } throws RuntimeException("db down")
        every { repo.findByFarmIdOrderByForecastAtAsc(farmId) } returns listOf(stored)

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException::class.java) {
            live.forecast(AccessScope(DemoTenant.ID, setOf(farmId), "OPERATOR"), farmId)
        }
        verify(exactly = 0) { repo.deleteAll(any<List<WeatherEntity>>()) }
    }
}
