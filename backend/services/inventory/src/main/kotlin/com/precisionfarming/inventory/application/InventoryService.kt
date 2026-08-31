package com.precisionfarming.inventory.application

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.inventory.infrastructure.ItemEntity
import com.precisionfarming.inventory.infrastructure.ItemJpaRepository
import com.precisionfarming.inventory.infrastructure.MovementEntity
import com.precisionfarming.inventory.infrastructure.MovementJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ItemDto(
    val id: UUID, val farmId: UUID, val name: String, val category: String,
    val unit: String, val quantity: BigDecimal, val reserved: BigDecimal,
)
data class UpsertItem(val farmId: UUID, val name: String, val category: String, val unit: String, val quantity: BigDecimal)
data class MovementCmd(val itemId: UUID, val type: String, val quantity: BigDecimal, val reference: String?)

@Service
class InventoryService(
    private val items: ItemJpaRepository,
    private val movements: MovementJpaRepository,
) {
    fun list(farmId: UUID?) = (farmId?.let { items.findByFarmId(it) } ?: items.findAll()).map { it.toDto() }

    @Transactional
    fun create(cmd: UpsertItem) =
        items.save(ItemEntity(UUID.randomUUID(), cmd.farmId, cmd.name, cmd.category, cmd.unit, cmd.quantity)).toDto()

    @Transactional
    fun move(cmd: MovementCmd): ItemDto {
        val item = items.findById(cmd.itemId).orElseThrow { NotFoundException("ITEM_NOT_FOUND", "Item not found") }
        when (cmd.type) {
            "RESERVE" -> {
                if (item.quantity - item.reserved < cmd.quantity) {
                    throw ConflictException("INSUFFICIENT_STOCK", "Not enough stock to reserve")
                }
                item.reserved += cmd.quantity
            }
            "RELEASE" -> item.reserved = (item.reserved - cmd.quantity).max(BigDecimal.ZERO)
            "CONSUME" -> {
                item.quantity -= cmd.quantity
                item.reserved = (item.reserved - cmd.quantity).max(BigDecimal.ZERO)
            }
            "IN" -> item.quantity += cmd.quantity
            else -> throw ConflictException("UNKNOWN_MOVEMENT", "Unknown movement type")
        }
        movements.save(MovementEntity(UUID.randomUUID(), item.id, cmd.type, cmd.quantity, Instant.now(), cmd.reference))
        return items.save(item).toDto()
    }

    @Transactional
    fun seed() {
        data class Row(val key: String, val farm: String, val name: String, val cat: String, val unit: String, val qty: String)
        val rows = listOf(
            Row("item-001", "farm-001", "Glifosato", "DEFENSIVO", "L", "420"),
            Row("item-002", "farm-001", "Ureia", "FERTILIZANTE", "KG", "1800"),
            Row("item-003", "farm-002", "Semente soja", "SEMENTE", "KG", "900"),
        )
        val existing = items.findAllById(rows.map { DemoIds.uuid(it.key) }).map { it.id }.toHashSet()
        items.saveAll(
            rows.filter { DemoIds.uuid(it.key) !in existing }.map { r ->
                ItemEntity(DemoIds.uuid(r.key), DemoIds.uuid(r.farm), r.name, r.cat, r.unit, BigDecimal(r.qty))
            },
        )
    }

    private fun ItemEntity.toDto() = ItemDto(id, farmId, name, category, unit, quantity, reserved)
}

@Service
class InventorySeed(
    private val svc: InventoryService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedInventory() = ApplicationRunner { if (seed) svc.seed() }
}
