package com.precisionfarming.inventory.application

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.cappedNewest
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.inventory.domain.InventoryUnits
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
import java.time.temporal.ChronoUnit
import java.util.UUID

data class ItemDto(
    val id: UUID, val farmId: UUID, val name: String, val category: String,
    val unit: String, val quantity: BigDecimal, val reserved: BigDecimal,
)
data class UpsertItem(val farmId: UUID, val name: String, val category: String, val unit: String, val quantity: BigDecimal)
data class PatchItem(val farmId: UUID, val name: String, val category: String, val unit: String)
data class MovementCmd(
    val itemId: UUID,
    val type: String,
    val quantity: BigDecimal,
    val reference: String?,
    val stepKey: String? = null,
)
data class MovementDto(
    val id: UUID,
    val itemId: UUID,
    val type: String,
    val quantity: BigDecimal,
    val occurredAt: Instant,
    val reference: String?,
)

@Service
class InventoryService(
    private val items: ItemJpaRepository,
    private val movements: MovementJpaRepository,
) {
    fun list(scope: AccessScope, farmId: UUID?) =
        items.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun get(scope: AccessScope, id: UUID): ItemDto {
        val e = items.findById(id).orElseThrow { NotFoundException("ITEM_NOT_FOUND", "Item not found") }
        scope.requireEntityFarm(e.farmId)
        return e.toDto()
    }

    fun listMovements(scope: AccessScope, itemId: UUID): List<MovementDto> {
        val item = items.findById(itemId).orElseThrow { NotFoundException("ITEM_NOT_FOUND", "Item not found") }
        scope.requireFarmRead(item.farmId, "ITEM_NOT_FOUND", "Item not found")
        return movements.findByItemIdOrderByOccurredAtAsc(itemId).cappedNewest().map { it.toDto() }
    }

    @Transactional
    fun create(scope: AccessScope, cmd: UpsertItem): ItemDto {
        scope.requireFarm(cmd.farmId)
        val unit = InventoryUnits.requireKnown(cmd.unit)
        val saved = items.save(
            ItemEntity(UUID.randomUUID(), cmd.farmId, cmd.name, cmd.category, unit, cmd.quantity),
        ).toDto()
        return saved
    }

    @Transactional
    fun patch(scope: AccessScope, id: UUID, cmd: PatchItem): ItemDto {
        val item = items.findById(id).orElseThrow { NotFoundException("ITEM_NOT_FOUND", "Item not found") }
        scope.requireEntityFarm(item.farmId)
        scope.requireFarm(cmd.farmId)
        item.farmId = cmd.farmId
        item.name = cmd.name
        item.category = cmd.category
        val stock = InventoryUnits.apply(item.unit, cmd.unit, item.quantity, item.reserved)
        item.unit = stock.unit
        item.quantity = stock.quantity
        item.reserved = stock.reserved
        val saved = items.save(item).toDto()
        return saved
    }

    @Transactional
    fun move(scope: AccessScope, cmd: MovementCmd): ItemDto {
        val item = items.findById(cmd.itemId).orElseThrow { NotFoundException("ITEM_NOT_FOUND", "Item not found") }
        scope.requireEntityFarm(item.farmId)
        val stepKey = cmd.stepKey?.takeIf { it.isNotBlank() }
        if (stepKey != null && movements.existsByItemIdAndTypeAndStepKey(item.id, cmd.type, stepKey)) {
            return item.toDto()
        }
        if (stepKey != null && !undoTargetExists(item.id, cmd.type, stepKey)) {
            return item.toDto()
        }
        when (cmd.type) {
            "RESERVE" -> {
                if (item.quantity - item.reserved < cmd.quantity) {
                    throw ConflictException("INSUFFICIENT_STOCK", "Not enough stock to reserve")
                }
                item.reserved += cmd.quantity
            }
            "RELEASE" -> item.reserved = (item.reserved - cmd.quantity).max(BigDecimal.ZERO)
            "CONSUME" -> {
                if (item.quantity < cmd.quantity) {
                    throw ConflictException("INSUFFICIENT_STOCK", "Not enough stock to consume")
                }
                item.quantity -= cmd.quantity
                item.reserved = (item.reserved - cmd.quantity).max(BigDecimal.ZERO)
            }
            "IN" -> item.quantity += cmd.quantity
            else -> throw ConflictException("UNKNOWN_MOVEMENT", "Unknown movement type")
        }
        try {
            movements.save(MovementEntity(UUID.randomUUID(), item.id, cmd.type, cmd.quantity, Instant.now(), cmd.reference, stepKey))
            val saved = items.save(item).toDto()
            // The unique step_key index is checked on flush, not on save().
            movements.flush()
            return saved
        } catch (_: org.springframework.dao.DataIntegrityViolationException) {
            throw ConflictException("STEP_ALREADY_APPLIED", "Movement step already applied")
        }
    }

    /** Compensation is a no-op until the forward step is stored, so a lost HTTP success cannot be applied twice. */
    private fun undoTargetExists(itemId: UUID, type: String, stepKey: String): Boolean {
        val forward = when {
            type == "RELEASE" && stepKey.endsWith(":UNDO-RESERVE") ->
                "RESERVE" to stepKey.removeSuffix(":UNDO-RESERVE") + ":RESERVE"
            type == "RELEASE" && stepKey.endsWith(":RELEASE-UNUSED") ->
                "CONSUME" to stepKey.removeSuffix(":RELEASE-UNUSED") + ":CONSUME"
            type == "IN" && stepKey.endsWith(":UNDO-CONSUME") ->
                "CONSUME" to stepKey.removeSuffix(":UNDO-CONSUME") + ":CONSUME"
            type == "RESERVE" && ":RESTORE-RESERVE" in stepKey ->
                "IN" to stepKey.substringBefore(":RESTORE-RESERVE") + ":UNDO-CONSUME"
            else -> return true
        }
        return movements.existsByItemIdAndTypeAndStepKey(itemId, forward.first, forward.second)
    }

    @Transactional
    fun seed() {
        data class Row(val key: String, val farm: String, val name: String, val cat: String, val unit: String, val qty: String)
        val rows = listOf(
            Row("item-001", "farm-001", "GLYPHOSATE", "PESTICIDE", "L", "420"),
            Row("item-002", "farm-001", "UREA", "FERTILIZER", "KG", "1800"),
            Row("item-003", "farm-002", "SOY_SEED", "SEED", "KG", "900"),
            Row("item-004", "farm-001", "DIESEL_S10", "FUEL", "L", "5200"),
            Row("item-005", "farm-002", "TWO_FOUR_D", "PESTICIDE", "L", "310"),
            Row("item-006", "farm-002", "OIL_FILTER", "PART", "UN", "24"),
            Row("item-007", "farm-003", "CORN_SEED", "SEED", "KG", "1100"),
            Row("item-008", "farm-003", "MAP", "FERTILIZER", "KG", "2400"),
            Row("item-009", "farm-004", "INSECTICIDE", "PESTICIDE", "L", "180"),
            Row("item-010", "farm-004", "DIESEL_S10", "FUEL", "L", "3800"),
            Row("item-011", "farm-005", "KCL", "FERTILIZER", "KG", "1600"),
            Row("item-012", "farm-005", "DRIVE_BELT", "PART", "UN", "12"),
            Row("item-013", "farm-006", "COTTON_SEED", "SEED", "KG", "640"),
            Row("item-014", "farm-006", "PRE_EMERGENT", "PESTICIDE", "L", "220"),
            Row("item-015", "farm-007", "UREA", "FERTILIZER", "KG", "980"),
            Row("item-016", "farm-008", "HYDRAULIC_OIL", "FUEL", "L", "450"),
        )
        val existing = items.findAllById(rows.map { DemoIds.uuid(it.key) }).associateBy { it.id }
        items.saveAll(
            rows.map { r ->
                val id = DemoIds.uuid(r.key)
                val found = existing[id]
                if (found != null) {
                    found.farmId = DemoIds.uuid(r.farm)
                    found.name = r.name
                    found.category = r.cat
                    found.unit = r.unit
                    found
                } else {
                    ItemEntity(id, DemoIds.uuid(r.farm), r.name, r.cat, r.unit, BigDecimal(r.qty))
                }
            },
        )
        seedMovements(rows.map { it.key })
    }

    private fun seedMovements(itemKeys: List<String>) {
        val ids = itemKeys.flatMap { key -> (0..6).map { d -> DemoIds.uuid("move-$key-d$d") } }
        val existing = movements.findAllById(ids).associateBy { it.id }
        val today = Instant.now().truncatedTo(ChronoUnit.DAYS)
        movements.saveAll(
            itemKeys.flatMap { key ->
                (0..6).map { d ->
                    val id = DemoIds.uuid("move-$key-d$d")
                    val occurredAt = today.minus((6 - d).toLong(), ChronoUnit.DAYS)
                    val found = existing[id]
                    if (found != null) {
                        found.occurredAt = occurredAt
                        found
                    } else {
                        MovementEntity(
                            id,
                            DemoIds.uuid(key),
                            "CONSUME",
                            BigDecimal(8 + d),
                            occurredAt,
                            "seed:consume:$key:d$d",
                        )
                    }
                }
            },
        )
    }

    private fun ItemEntity.toDto() = ItemDto(id, farmId, name, category, unit, quantity, reserved)
    private fun MovementEntity.toDto() = MovementDto(id, itemId, type, quantity, occurredAt, reference)
}

@Service
class InventorySeed(
    private val svc: InventoryService,
    private val gate: com.precisionfarming.security.DemoSeedGate,
) {
    @Bean
    fun seedInventory() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
