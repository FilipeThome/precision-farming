package com.precisionfarming.harvest

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HarvestDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("harvest-001"), DemoIds.uuid("harvest-001"))
    }
}
