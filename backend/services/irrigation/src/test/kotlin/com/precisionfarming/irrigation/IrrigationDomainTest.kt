package com.precisionfarming.irrigation

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IrrigationDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("irrigation-001"), DemoIds.uuid("irrigation-001"))
    }
}
