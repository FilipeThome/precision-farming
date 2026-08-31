package com.precisionfarming.inventory

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("inventory-001"), DemoIds.uuid("inventory-001"))
    }
}
