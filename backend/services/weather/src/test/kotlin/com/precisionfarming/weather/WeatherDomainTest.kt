package com.precisionfarming.weather

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("weather-001"), DemoIds.uuid("weather-001"))
    }
}
