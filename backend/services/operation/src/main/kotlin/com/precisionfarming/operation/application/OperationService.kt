package com.precisionfarming.operation.application

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.operation.infrastructure.InventorySagaClient
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoFieldFarms
import com.precisionfarming.security.DemoItemFarms
import com.precisionfarming.security.DemoMachineFarms
import com.precisionfarming.operation.infrastructure.OperationEntity
import com.precisionfarming.operation.infrastructure.OperationJpaRepository
import com.precisionfarming.operation.infrastructure.SagaEntity
import com.precisionfarming.operation.infrastructure.SagaJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
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
    private val txManager: PlatformTransactionManager,
) {
    private fun <T> inTx(block: () -> T): T = TransactionTemplate(txManager).execute { block() }!!

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
        DemoFieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        cmd.machineId?.let { DemoMachineFarms.requireBelongsToFarm(it, cmd.farmId) }
        cmd.itemId?.let { DemoItemFarms.requireBelongsToFarm(it, cmd.farmId) }
        return repo.save(
            OperationEntity(
                UUID.randomUUID(), cmd.fieldId, cmd.farmId, cmd.type, "PLANNED",
                cmd.plannedStart, cmd.plannedEnd, null, null, cmd.machineId, null, cmd.itemId, cmd.itemQuantity,
            ),
        ).toDto()
    }

    fun start(scope: AccessScope, id: UUID): OperationDto {
        val (needsReserve, previousStatus) = inTx {
            val op = load(scope, id)
            if (op.status != "PLANNED" && op.status != "PAUSED") {
                throw ConflictException("OPERATION_STATE_CONFLICT", "Operation cannot be started from ${op.status}")
            }
            val reserve = op.status == "PLANNED"
            val previous = op.status
            op.status = "STARTING"
            repo.save(op)
            sagas.save(
                SagaEntity(UUID.randomUUID(), op.id, "StartOperationSaga", "STARTED", null, Instant.now(), Instant.now()),
            )
            reserve to previous
        }
        var reserved = false
        try {
            if (needsReserve) {
                reserved = inventoryMove(load(scope, id), "RESERVE")
            }
            return inTx { persistStart(scope, id, reserved) }
        } catch (ex: Exception) {
            if (ex is ConflictException && ex.code == "OPERATION_STATE_CONFLICT") throw ex
            if (reserved) {
                try {
                    inventoryMove(load(scope, id), "RELEASE")
                } catch (compensateEx: Exception) {
                    inTx { markCompensated(id, "StartOperationSaga", "${ex.message}; RELEASE failed: ${compensateEx.message}") }
                }
            }
            inTx { revertStatus(scope, id, "STARTING", previousStatus) }
            if (ex is ConflictException && ex.code != "SAGA_FAILED") throw ex
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

    fun complete(scope: AccessScope, id: UUID): OperationDto {
        val previousStatus = inTx {
            val op = load(scope, id)
            if (op.status != "IN_PROGRESS" && op.status != "PAUSED") {
                throw ConflictException("OPERATION_STATE_CONFLICT", "Cannot complete from ${op.status}")
            }
            val previous = op.status
            op.status = "COMPLETING"
            repo.save(op)
            sagas.save(
                SagaEntity(UUID.randomUUID(), op.id, "CompleteOperationSaga", "STARTED", null, Instant.now(), Instant.now()),
            )
            previous
        }
        var consumed = false
        try {
            consumed = inventoryMove(load(scope, id), "CONSUME")
            return inTx { persistComplete(scope, id, consumed) }
        } catch (ex: Exception) {
            if (ex is ConflictException && ex.code == "OPERATION_STATE_CONFLICT") throw ex
            if (consumed) {
                try {
                    inventoryMove(load(scope, id), "IN")
                    inventoryMove(load(scope, id), "RESERVE")
                } catch (compensateEx: Exception) {
                    inTx { markCompensated(id, "CompleteOperationSaga", "${ex.message}; restore failed: ${compensateEx.message}") }
                }
            }
            inTx { revertStatus(scope, id, "COMPLETING", previousStatus) }
            if (ex is ConflictException && ex.code != "SAGA_FAILED") throw ex
            throw ConflictException("SAGA_FAILED", ex.message ?: "Failed to complete operation")
        }
    }

    private fun persistStart(scope: AccessScope, id: UUID, reserved: Boolean): OperationDto {
        val op = load(scope, id)
        if (op.status != "STARTING") {
            throw ConflictException("OPERATION_STATE_CONFLICT", "Operation cannot be started from ${op.status}")
        }
        val saga = sagas.save(
            SagaEntity(UUID.randomUUID(), op.id, "StartOperationSaga", if (reserved) "INVENTORY_RESERVED" else "STARTED", null, Instant.now(), Instant.now()),
        )
        op.status = "IN_PROGRESS"
        op.actualStart = op.actualStart ?: Instant.now()
        op.pauseReason = null
        saga.state = "COMPLETED"
        sagas.save(saga)
        return repo.save(op).toDto()
    }

    private fun persistComplete(scope: AccessScope, id: UUID, consumed: Boolean): OperationDto {
        val op = load(scope, id)
        if (op.status != "COMPLETING") {
            throw ConflictException("OPERATION_STATE_CONFLICT", "Cannot complete from ${op.status}")
        }
        val saga = sagas.save(
            SagaEntity(UUID.randomUUID(), op.id, "CompleteOperationSaga", if (consumed) "INVENTORY_CONSUMED" else "STARTED", null, Instant.now(), Instant.now()),
        )
        op.status = "COMPLETED"
        op.actualEnd = Instant.now()
        saga.state = "COMPLETED"
        sagas.save(saga)
        return repo.save(op).toDto()
    }

    private fun revertStatus(scope: AccessScope, id: UUID, expected: String, previous: String) {
        val op = load(scope, id)
        if (op.status == expected) {
            op.status = previous
            repo.save(op)
        }
    }

    private fun markCompensated(operationId: UUID, name: String, payload: String) {
        sagas.save(
            SagaEntity(UUID.randomUUID(), operationId, name, "COMPENSATED", payload, Instant.now(), Instant.now()),
        )
    }

    private fun inventoryMove(op: OperationEntity, type: String): Boolean {
        val itemId = op.itemId ?: return false
        val qty = op.itemQuantity ?: return false
        inventory.move(itemId, type, qty, op.id.toString(), op.farmId)
        return true
    }

    private fun load(scope: AccessScope, id: UUID): OperationEntity {
        val op = repo.findById(id).orElseThrow { NotFoundException("OPERATION_NOT_FOUND", "Not found") }
        scope.requireEntityFarm(op.farmId)
        return op
    }

    @Transactional
    fun seed() {
        data class Row(
            val key: String, val field: String, val farm: String, val type: String,
            val status: String, val machine: String?, val item: String,
        )
        val now = Instant.now()
        val rows = listOf(
            Row("op-001", "field-001", "farm-001", "Plantio", "COMPLETED", "machine-001", "item-001"),
            Row("op-002", "field-001", "farm-001", "Pulverização", "IN_PROGRESS", "machine-002", "item-001"),
            Row("op-003", "field-002", "farm-001", "Adubação", "PLANNED", "machine-001", "item-002"),
            Row("op-004", "field-003", "farm-001", "Inspeção", "PLANNED", null, "item-001"),
            Row("op-005", "field-004", "farm-002", "Plantio", "PAUSED", "machine-004", "item-003"),
            Row("op-006", "field-005", "farm-002", "Pulverização", "IN_PROGRESS", "machine-006", "item-005"),
            Row("op-007", "field-006", "farm-003", "Plantio", "COMPLETED", "machine-005", "item-007"),
            Row("op-008", "field-007", "farm-003", "Adubação", "PAUSED", "machine-005", "item-008"),
            Row("op-009", "field-008", "farm-003", "Pulverização", "PLANNED", null, "item-008"),
            Row("op-010", "field-009", "farm-004", "Plantio", "IN_PROGRESS", "machine-007", "item-009"),
            Row("op-011", "field-010", "farm-004", "Colheita", "PLANNED", "machine-008", "item-010"),
            Row("op-012", "field-011", "farm-004", "Adubação", "COMPLETED", "machine-007", "item-009"),
            Row("op-013", "field-012", "farm-005", "Plantio", "PLANNED", "machine-009", "item-011"),
            Row("op-014", "field-013", "farm-005", "Pulverização", "PAUSED", "machine-009", "item-011"),
            Row("op-015", "field-016", "farm-005", "Inspeção", "COMPLETED", null, "item-012"),
            Row("op-016", "field-017", "farm-006", "Plantio", "IN_PROGRESS", "machine-010", "item-013"),
            Row("op-017", "field-018", "farm-006", "Adubação", "PLANNED", "machine-010", "item-014"),
            Row("op-018", "field-019", "farm-007", "Pulverização", "PAUSED", "machine-011", "item-015"),
            Row("op-019", "field-020", "farm-007", "Plantio", "COMPLETED", null, "item-015"),
            Row("op-020", "field-021", "farm-008", "Adubação", "IN_PROGRESS", "machine-012", "item-016"),
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
                    DemoIds.uuid(r.item),
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
