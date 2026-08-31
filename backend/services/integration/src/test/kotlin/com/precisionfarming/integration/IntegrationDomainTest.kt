package com.precisionfarming.integration

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntegrationDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("integration-001"), DemoIds.uuid("integration-001"))
    }
}
