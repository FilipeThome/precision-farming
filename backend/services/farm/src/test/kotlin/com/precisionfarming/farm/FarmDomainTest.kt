package com.precisionfarming.farm

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FarmDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("farm-001"), DemoIds.uuid("farm-001"))
    }
}
