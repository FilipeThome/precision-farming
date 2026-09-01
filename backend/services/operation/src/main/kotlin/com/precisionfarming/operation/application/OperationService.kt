package com.precisionfarming.operation.application

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.operation.infrastructure.InventorySagaClient
import com.precisionfarming.security.AccessScope
import com.precisionfarming.operation.infrastructure.OperationEntity
import com.precisionfarming.operation.infrastructure.OperationJpaRepository
import com.precisionfarming.operation.infrastructure.SagaEntity
import com.precisionfarming.operation.infrastructure.SagaJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class OperationDto(
    val id: UUID, val fieldId: UUID, val farmId: UUID, val type: String, val status: String,
    val plannedStart: Instant?, val plannedEnd: Instant?, val actualStart: Instant?, val actualEnd: Instant?,
    val machineId: UUID?, val pauseReason: String?, val itemId: UUID?, val itemQuantity: BigDecimal?,
)
data class CreateOperation(
    val fieldId: UUID, val farmId: UUID, val type: String,
    val plannedStart: Instant?, val plannedEnd: Instant?,
    val machineId: UUID?, val itemId: UUID?, val itemQuantity: BigDecimal?,
)

@Service
class OperationService(
    private val repo: OperationJpaRepository,
    private val sagas: SagaJpaRepository,
    private val inventory: InventorySagaClient,
) {

    fun list(scope: AccessScope, farmId: UUID? = null) =
        repo.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun get(scope: AccessScope, id: UUID): OperationDto {
        val e = repo.findById(id).orElseThrow { NotFoundException("OPERATION_NOT_FOUND", "Not found") }
        scope.requireEntityFarm(e.farmId)
        return e.toDto()
    }

    @Transactional
    fun create(scope: AccessScope, cmd: CreateOperation): OperationDto {
        scope.requireFarm(cmd.farmId)
        return repo.save(
            OperationEntity(
                UUID.randomUUID(), cmd.fieldId, cmd.farmId, cmd.type, "PLANNED",
                cmd.plannedStart, cmd.plannedEnd, null, null, cmd.machineId, null, cmd.itemId, cmd.itemQuantity,
            ),
        ).toDto()
    }

    @Transactional
    fun start(scope: AccessScope, id: UUID): OperationDto {
        val op = load(scope, id)
        if (op.status != "PLANNED" && op.status != "PAUSED") {
            throw ConflictException("OPERATION_STATE_CONFLICT", "Operation cannot be started from ${op.status}")
        }
        val needsReserve = op.status == "PLANNED"
        val saga = sagas.save(
            SagaEntity(UUID.randomUUID(), op.id, "StartOperationSaga", "STARTED", null, Instant.now(), Instant.now()),
        )
        var reserved = false
        try {
            if (needsReserve) {
                reserved = inventoryMove(op, "RESERVE")
                if (reserved) saga.state = "INVENTORY_RESERVED"
            }
            op.status = "IN_PROGRESS"
            op.actualStart = op.actualStart ?: Instant.now()
            op.pauseReason = null
            saga.state = "COMPLETED"
            sagas.save(saga)
            return repo.save(op).toDto()
        } catch (ex: Exception) {
            if (reserved) {
                try {
                    inventoryMove(op, "RELEASE")
                } catch (compensateEx: Exception) {
                    saga.payload = "${ex.message}; RELEASE failed: ${compensateEx.message}"
                }
            }
            if (saga.payload == null) saga.payload = ex.message
            saga.state = "COMPENSATED"
            saga.updatedAt = Instant.now()
            sagas.save(saga)
            throw ConflictException("SAGA_FAILED", ex.message ?: "Failed to start operation")
        }
    }

    @Transactional
    fun pause(scope: AccessScope, id: UUID, reason: String?): OperationDto {
        val op = load(scope, id)
        if (op.status != "IN_PROGRESS") {
            throw ConflictException("OPERATION_STATE_CONFLICT", "Cannot pause from ${op.status}")
        }
        op.status = "PAUSED"
        op.pauseReason = reason
        return repo.save(op).toDto()
    }

    @Transactional
    fun complete(scope: AccessScope, id: UUID): OperationDto {
        val op = load(scope, id)
        if (op.status != "IN_PROGRESS" && op.status != "PAUSED") {
            throw ConflictException("OPERATION_STATE_CONFLICT", "Cannot complete from ${op.status}")
        }
        val saga = sagas.save(
            SagaEntity(UUID.randomUUID(), op.id, "CompleteOperationSaga", "STARTED", null, Instant.now(), Instant.now()),
        )
        var consumed = false
        try {
            consumed = inventoryMove(op, "CONSUME")
            if (consumed) saga.state = "INVENTORY_CONSUMED"
            op.status = "COMPLETED"
            op.actualEnd = Instant.now()
            saga.state = "COMPLETED"
            sagas.save(saga)
            return repo.save(op).toDto()
        } catch (ex: Exception) {
            if (consumed) {
                try {
                    inventoryMove(op, "IN")
                    inventoryMove(op, "RESERVE")
                } catch (compensateEx: Exception) {
                    saga.payload = "${ex.message}; restore failed: ${compensateEx.message}"
                }
            }
            if (saga.payload == null) saga.payload = ex.message
            saga.state = "COMPENSATED"
            saga.updatedAt = Instant.now()
            sagas.save(saga)
            throw ConflictException("SAGA_FAILED", ex.message ?: "Failed to complete operation")
        }
    }

    private fun inventoryMove(op: OperationEntity, type: String): Boolean {
        val itemId = op.itemId ?: return false
        val qty = op.itemQuantity ?: return false
        inventory.move(itemId, type, qty, op.id.toString())
        return true
    }

    private fun load(scope: AccessScope, id: UUID): OperationEntity {
        val op = repo.findById(id).orElseThrow { NotFoundException("OPERATION_NOT_FOUND", "Not found") }
        scope.requireEntityFarm(op.farmId)
        return op
    }

    @Transactional
    fun seed() {
        data class Row(val key: String, val field: String, val farm: String, val type: String, val status: String, val machine: String?)
        val now = Instant.now()
        val rows = listOf(
            Row("op-001", "field-001", "farm-001", "Plantio", "COMPLETED", "machine-001"),
            Row("op-002", "field-001", "farm-001", "Pulverização", "IN_PROGRESS", "machine-002"),
            Row("op-003", "field-002", "farm-001", "Adubação", "PLANNED", "machine-001"),
            Row("op-004", "field-003", "farm-001", "Inspeção", "PLANNED", null),
            Row("op-005", "field-004", "farm-002", "Plantio", "PAUSED", "machine-004"),
        )
        val existing = repo.findAllById(rows.map { DemoIds.uuid(it.key) }).map { it.id }.toHashSet()
        repo.saveAll(
            rows.filter { DemoIds.uuid(it.key) !in existing }.map { r ->
                OperationEntity(
                    DemoIds.uuid(r.key), DemoIds.uuid(r.field), DemoIds.uuid(r.farm), r.type, r.status,
                    now.minus(2, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS),
                    if (r.status != "PLANNED") now.minus(1, ChronoUnit.DAYS) else null,
                    if (r.status == "COMPLETED") now.minus(2, ChronoUnit.HOURS) else null,
                    r.machine?.let { DemoIds.uuid(it) },
                    if (r.status == "PAUSED") "Chuva" else null,
                    DemoIds.uuid("item-001"),
                    BigDecimal("20"),
                )
            },
        )
    }

    private fun OperationEntity.toDto() = OperationDto(
        id, fieldId, farmId, type, status, plannedStart, plannedEnd, actualStart, actualEnd, machineId, pauseReason, itemId, itemQuantity,
    )
}

@Service
class OperationSeed(
    private val svc: OperationService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedOps() = ApplicationRunner { if (seed) svc.seed() }
}
