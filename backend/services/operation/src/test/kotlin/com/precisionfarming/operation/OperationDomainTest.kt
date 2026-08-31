package com.precisionfarming.operation

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OperationDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("operation-001"), DemoIds.uuid("operation-001"))
    }
}
