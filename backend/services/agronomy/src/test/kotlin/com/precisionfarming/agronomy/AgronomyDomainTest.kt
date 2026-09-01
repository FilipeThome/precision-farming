package com.precisionfarming.agronomy

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AgronomyDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("agronomy-001"), DemoIds.uuid("agronomy-001"))
    }
}
