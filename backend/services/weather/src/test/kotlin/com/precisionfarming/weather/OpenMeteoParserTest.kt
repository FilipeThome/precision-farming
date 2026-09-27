package com.precisionfarming.weather

import com.precisionfarming.common.DemoIds
import com.precisionfarming.weather.application.parseOpenMeteoDaily
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OpenMeteoParserTest {
    @Test
    fun mapsDailyArraysOntoForecastRows() {
        val farmId = DemoIds.uuid("farm-001")
        val body = """
            {
              "daily": {
                "time": ["2026-09-24", "2026-09-25"],
                "temperature_2m_max": [31.2, 28.0],
                "temperature_2m_min": [19.4, 17.0],
                "precipitation_sum": [0.0, 12.6],
                "precipitation_probability_max": [10, 80],
                "wind_speed_10m_max": [11.0, 24.0],
                "relative_humidity_2m_mean": [62, 88]
              }
            }
        """.trimIndent()

        val rows = parseOpenMeteoDaily(farmId, body)

        assertEquals(2, rows.size)
        assertEquals("open-meteo", rows[0].vintage)
        assertEquals(BigDecimal("31.2"), rows[0].temperatureMax)
        assertEquals("FAVORABLE", rows[0].sprayingWindow)
        assertEquals("UNFAVORABLE", rows[1].sprayingWindow)
        assertEquals(farmId, rows[1].farmId)
    }
}
