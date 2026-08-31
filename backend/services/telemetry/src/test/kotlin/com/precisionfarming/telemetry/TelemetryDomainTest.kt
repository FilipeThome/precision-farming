package com.precisionfarming.telemetry

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TelemetryDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("telemetry-001"), DemoIds.uuid("telemetry-001"))
    }
}
