package com.precisionfarming.operation.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "operations")
class OperationEntity(
    @Id val id: UUID,
    @Column(name = "field_id") var fieldId: UUID,
    @Column(name = "farm_id") var farmId: UUID,
    var type: String,
    var status: String,
    @Column(name = "planned_start") var plannedStart: Instant?,
    @Column(name = "planned_end") var plannedEnd: Instant?,
    @Column(name = "actual_start") var actualStart: Instant?,
    @Column(name = "actual_end") var actualEnd: Instant?,
    @Column(name = "machine_id") var machineId: UUID?,
    @Column(name = "pause_reason") var pauseReason: String?,
    @Column(name = "item_id") var itemId: UUID?,
    @Column(name = "item_quantity") var itemQuantity: BigDecimal?,
    @Column(name = "area_ha") var areaHa: BigDecimal? = null,
    @Version var version: Long = 0,
)

@Entity
@Table(name = "saga_instances")
class SagaEntity(
    @Id val id: UUID,
    @Column(name = "operation_id") val operationId: UUID,
    var type: String,
    var state: String,
    var payload: String?,
    @Column(name = "created_at") val createdAt: Instant,
    @Column(name = "updated_at") var updatedAt: Instant,
)

interface OperationJpaRepository : JpaRepository<OperationEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<OperationEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<OperationEntity>
    fun findByMachineIdAndFarmIdIn(machineId: UUID, farmIds: Collection<UUID>): List<OperationEntity>
}
interface SagaJpaRepository : JpaRepository<SagaEntity, UUID>
