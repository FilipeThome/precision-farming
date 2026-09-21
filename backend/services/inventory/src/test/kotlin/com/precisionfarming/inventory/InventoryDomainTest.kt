package com.precisionfarming.inventory

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.inventory.domain.InventoryUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class InventoryDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("inventory-001"), DemoIds.uuid("inventory-001"))
    }

    @Test
    fun convertsKilogramsToTonnes() {
        val next = InventoryUnits.apply("KG", "T", BigDecimal("100"), BigDecimal("20"))
        assertEquals("T", next.unit)
        assertEquals(0, BigDecimal("0.1").compareTo(next.quantity))
        assertEquals(0, BigDecimal("0.02").compareTo(next.reserved))
    }

    @Test
    fun keepsStockWhenUnitIsUnchanged() {
        val next = InventoryUnits.apply("kg", "KG", BigDecimal("100"), BigDecimal("20"))
        assertEquals("KG", next.unit)
        assertEquals(BigDecimal("100"), next.quantity)
        assertEquals(BigDecimal("20"), next.reserved)
    }

    @Test
    fun rejectsIncompatibleUnitChange() {
        val ex = assertThrows(ConflictException::class.java) {
            InventoryUnits.apply("KG", "L", BigDecimal("100"), BigDecimal.ZERO)
        }
        assertEquals("UNIT_CHANGE_UNSUPPORTED", ex.code)
    }

    @Test
    fun rejectsUnknownUnit() {
        val ex = assertThrows(DomainException::class.java) {
            InventoryUnits.requireKnown("FOO")
        }
        assertEquals("UNIT_UNSUPPORTED", ex.code)
    }

    @Test
    fun normalizesKnownUnit() {
        assertEquals("KG", InventoryUnits.requireKnown("kg"))
    }
}
