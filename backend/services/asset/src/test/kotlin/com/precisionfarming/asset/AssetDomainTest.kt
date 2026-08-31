package com.precisionfarming.asset

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AssetDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("asset-001"), DemoIds.uuid("asset-001"))
    }
}
