package com.precisionfarming.notification

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotificationDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("notification-001"), DemoIds.uuid("notification-001"))
    }
}
