package com.precisionfarming.finance

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FinanceDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("finance-001"), DemoIds.uuid("finance-001"))
    }
}
