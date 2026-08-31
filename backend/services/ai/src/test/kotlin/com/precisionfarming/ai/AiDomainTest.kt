package com.precisionfarming.ai

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("ai-001"), DemoIds.uuid("ai-001"))
    }
}
