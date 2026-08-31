package com.precisionfarming.alert

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AlertDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("alert-001"), DemoIds.uuid("alert-001"))
    }
}
