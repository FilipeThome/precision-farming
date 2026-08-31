package com.precisionfarming.inventory.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inventory_items")
class ItemEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") var farmId: UUID,
    var name: String,
    var category: String,
    var unit: String,
    var quantity: BigDecimal,
    var reserved: BigDecimal = BigDecimal.ZERO,
)

@Entity
@Table(name = "inventory_movements")
class MovementEntity(
    @Id val id: UUID,
    @Column(name = "item_id") val itemId: UUID,
    var type: String,
    var quantity: BigDecimal,
    @Column(name = "occurred_at") val occurredAt: Instant,
    var reference: String?,
)

interface ItemJpaRepository : JpaRepository<ItemEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<ItemEntity>
}

interface MovementJpaRepository : JpaRepository<MovementEntity, UUID>
