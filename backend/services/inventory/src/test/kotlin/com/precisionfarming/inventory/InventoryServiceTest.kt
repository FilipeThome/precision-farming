package com.precisionfarming.inventory

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.QueryLimits
import com.precisionfarming.inventory.application.InventoryService
import com.precisionfarming.inventory.application.MovementCmd
import com.precisionfarming.inventory.application.PatchItem
import com.precisionfarming.inventory.application.UpsertItem
import com.precisionfarming.inventory.infrastructure.ItemEntity
import com.precisionfarming.inventory.infrastructure.ItemJpaRepository
import com.precisionfarming.inventory.infrastructure.MovementEntity
import com.precisionfarming.inventory.infrastructure.MovementJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

class InventoryServiceTest {
    private val items = mockk<ItemJpaRepository>()
    private val movements = mockk<MovementJpaRepository>()
    private val svc = InventoryService(items, movements)

    private fun scopeFor(item: ItemEntity) = AccessScope(DemoTenant.ID, setOf(item.farmId), "OPERATOR")

    @Test
    fun seedRewritesExistingMovementTimestamps() {
        val id = DemoIds.uuid("move-item-001-d0")
        val existing = MovementEntity(
            id,
            DemoIds.uuid("item-001"),
            "CONSUME",
            BigDecimal("8"),
            Instant.parse("2020-01-01T00:00:00Z"),
            "seed:consume:item-001:d0",
        )
        every { items.findAllById(any<Iterable<UUID>>()) } returns emptyList()
        every { items.saveAll(any<Iterable<ItemEntity>>()) } answers { firstArg() }
        every { movements.findAllById(any<Iterable<UUID>>()) } returns listOf(existing)
        lateinit var saved: List<MovementEntity>
        every { movements.saveAll(any<Iterable<MovementEntity>>()) } answers {
            firstArg<Iterable<MovementEntity>>().toList().also { saved = it }
        }

        svc.seed()

        val updated = saved.first { it.id == id }
        val weekAgo = Instant.now().truncatedTo(ChronoUnit.DAYS).minus(8, ChronoUnit.DAYS)
        assertTrue(updated.occurredAt.isAfter(weekAgo))
    }

    @Test
    fun consumeRejectsWhenQuantityInsufficient() {
        val item = item(quantity = "10", reserved = "0")
        every { items.findById(item.id) } returns Optional.of(item)

        val ex = assertThrows(ConflictException::class.java) {
            svc.move(scopeFor(item), MovementCmd(item.id, "CONSUME", BigDecimal("20"), "op-1"))
        }
        assertEquals("INSUFFICIENT_STOCK", ex.code)
        assertEquals(BigDecimal("10"), item.quantity)
    }

    @Test
    fun listMovementsRejectsOtherFarm() {
        val item = item(quantity = "10", reserved = "0")
        every { items.findById(item.id) } returns Optional.of(item)
        val other = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "OPERATOR")
        val ex = assertThrows(com.precisionfarming.common.NotFoundException::class.java) {
            svc.listMovements(other, item.id)
        }
        assertEquals("ITEM_NOT_FOUND", ex.code)
    }

    @Test
    fun listMovementsNotFound() {
        val id = UUID.randomUUID()
        every { items.findById(id) } returns Optional.empty()
        val ex = assertThrows(com.precisionfarming.common.NotFoundException::class.java) {
            svc.listMovements(AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "OPERATOR"), id)
        }
        assertEquals("ITEM_NOT_FOUND", ex.code)
    }

    @Test
    fun listMovementsReturnsRowsForScopedFarm() {
        val item = item(quantity = "10", reserved = "0")
        val row = MovementEntity(UUID.randomUUID(), item.id, "CONSUME", BigDecimal("8"), java.time.Instant.parse("2026-09-01T00:00:00Z"), "seed")
        every { items.findById(item.id) } returns Optional.of(item)
        every { movements.findByItemIdOrderByOccurredAtAsc(item.id) } returns listOf(row)
        val dto = svc.listMovements(scopeFor(item), item.id)
        assertEquals(1, dto.size)
        assertEquals(BigDecimal("8"), dto.single().quantity)
    }

    @Test
    fun listMovementsKeepsNewestWhenCapped() {
        val item = item(quantity = "10", reserved = "0")
        val rows = (0 until QueryLimits.MAX_LIST + 5).map { i ->
            MovementEntity(
                UUID.randomUUID(),
                item.id,
                "CONSUME",
                BigDecimal(i),
                Instant.parse("2026-01-01T00:00:00Z").plus(i.toLong(), ChronoUnit.DAYS),
                "row-$i",
            )
        }
        every { items.findById(item.id) } returns Optional.of(item)
        every { movements.findByItemIdOrderByOccurredAtAsc(item.id) } returns rows
        val dto = svc.listMovements(scopeFor(item), item.id)
        assertEquals(QueryLimits.MAX_LIST, dto.size)
        assertEquals(BigDecimal(5), dto.first().quantity)
        assertEquals(BigDecimal(QueryLimits.MAX_LIST + 4), dto.last().quantity)
    }

    @Test
    fun moveRejectsOtherFarm() {
        val item = item(quantity = "10", reserved = "0")
        every { items.findById(item.id) } returns Optional.of(item)
        val other = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "OPERATOR")
        assertThrows(com.precisionfarming.common.ForbiddenException::class.java) {
            svc.move(other, MovementCmd(item.id, "IN", BigDecimal("1"), null))
        }
    }

    @Test
    fun consumeDecrementsQuantityWhenStockIsAvailable() {
        val item = item(quantity = "50", reserved = "20")
        every { items.findById(item.id) } returns Optional.of(item)
        every { movements.save(any()) } answers { firstArg<MovementEntity>() }
        every { items.save(any()) } answers { firstArg<ItemEntity>() }

        val dto = svc.move(scopeFor(item), MovementCmd(item.id, "CONSUME", BigDecimal("20"), "op-1"))

        assertEquals(BigDecimal("30"), dto.quantity)
        assertEquals(BigDecimal.ZERO, dto.reserved)
    }

    @Test
    fun createAndPatchUpdatesNameWithoutChangingStock() {
        every { items.save(any<ItemEntity>()) } answers { firstArg() }
        val farmId = UUID.randomUUID()
        val scope = AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER")
        val created = svc.create(scope, UpsertItem(farmId, "Urea", "FERTILIZER", "KG", BigDecimal("100")))
        assertEquals(BigDecimal("100"), created.quantity)

        val entity = ItemEntity(created.id, farmId, created.name, created.category, created.unit, created.quantity, created.reserved)
        every { items.findById(created.id) } returns Optional.of(entity)
        val patched = svc.patch(scope, created.id, PatchItem(farmId, "MAP", "FERTILIZER", "T"))
        assertEquals("MAP", patched.name)
        assertEquals("T", patched.unit)
        assertEquals(0, BigDecimal("0.1").compareTo(patched.quantity))
        assertEquals(BigDecimal.ZERO, patched.reserved)
    }

    @Test
    fun patchRejectsIncompatibleUnitChange() {
        every { items.save(any<ItemEntity>()) } answers { firstArg() }
        val farmId = UUID.randomUUID()
        val scope = AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER")
        val created = svc.create(scope, UpsertItem(farmId, "Urea", "FERTILIZER", "KG", BigDecimal("100")))
        val entity = ItemEntity(created.id, farmId, created.name, created.category, created.unit, created.quantity, created.reserved)
        every { items.findById(created.id) } returns Optional.of(entity)
        val ex = assertThrows(ConflictException::class.java) {
            svc.patch(scope, created.id, PatchItem(farmId, "Urea", "FERTILIZER", "L"))
        }
        assertEquals("UNIT_CHANGE_UNSUPPORTED", ex.code)
        assertEquals(BigDecimal("100"), entity.quantity)
        assertEquals("KG", entity.unit)
    }

    @Test
    fun patchRejectsOtherFarm() {
        val item = item(quantity = "10", reserved = "0")
        every { items.findById(item.id) } returns Optional.of(item)
        val other = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "FARM_MANAGER")
        assertThrows(com.precisionfarming.common.ForbiddenException::class.java) {
            svc.patch(other, item.id, PatchItem(item.farmId, "X", "SEED", "KG"))
        }
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
