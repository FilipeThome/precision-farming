package com.precisionfarming.inventory

import com.precisionfarming.common.ConflictException
import com.precisionfarming.inventory.application.InventoryService
import com.precisionfarming.inventory.application.MovementCmd
import com.precisionfarming.inventory.infrastructure.ItemEntity
import com.precisionfarming.inventory.infrastructure.ItemJpaRepository
import com.precisionfarming.inventory.infrastructure.MovementEntity
import com.precisionfarming.inventory.infrastructure.MovementJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class InventoryServiceTest {
    private val items = mockk<ItemJpaRepository>()
    private val movements = mockk<MovementJpaRepository>()
    private val svc = InventoryService(items, movements)

    @Test
    fun consumeRejectsWhenQuantityInsufficient() {
        val item = item(quantity = "10", reserved = "0")
        every { items.findById(item.id) } returns Optional.of(item)

        val ex = assertThrows(ConflictException::class.java) {
            svc.move(MovementCmd(item.id, "CONSUME", BigDecimal("20"), "op-1"))
        }
        assertEquals("INSUFFICIENT_STOCK", ex.code)
        assertEquals(BigDecimal("10"), item.quantity)
    }

    @Test
    fun consumeDecrementsQuantityWhenStockIsAvailable() {
        val item = item(quantity = "50", reserved = "20")
        every { items.findById(item.id) } returns Optional.of(item)
        every { movements.save(any()) } answers { firstArg<MovementEntity>() }
        every { items.save(any()) } answers { firstArg<ItemEntity>() }

        val dto = svc.move(MovementCmd(item.id, "CONSUME", BigDecimal("20"), "op-1"))

        assertEquals(BigDecimal("30"), dto.quantity)
        assertEquals(BigDecimal.ZERO, dto.reserved)
    }

    private fun item(quantity: String, reserved: String) = ItemEntity(
        id = UUID.randomUUID(),
        farmId = UUID.randomUUID(),
        name = "Glifosato",
        category = "DEFENSIVO",
        unit = "L",
        quantity = BigDecimal(quantity),
        reserved = BigDecimal(reserved),
    )
}
