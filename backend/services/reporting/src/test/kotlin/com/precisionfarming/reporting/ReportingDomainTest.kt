package com.precisionfarming.reporting

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReportingDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("reporting-001"), DemoIds.uuid("reporting-001"))
    }
}
