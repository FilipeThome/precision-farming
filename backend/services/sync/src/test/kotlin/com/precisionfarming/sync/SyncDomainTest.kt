package com.precisionfarming.sync

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("sync-001"), DemoIds.uuid("sync-001"))
    }
}
