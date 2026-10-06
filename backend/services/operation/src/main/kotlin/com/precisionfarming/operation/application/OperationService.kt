package com.precisionfarming.operation.application

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.QueryLimits
import com.precisionfarming.operation.infrastructure.AgronomyPrescriptionClient
import com.precisionfarming.operation.infrastructure.InventorySagaClient
import com.precisionfarming.operation.infrastructure.InventoryStepResult
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.FieldFarmGuard
import com.precisionfarming.security.ItemFarmGuard
import com.precisionfarming.security.MachineFarmGuard
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
    val areaHa: BigDecimal?,
    val prescriptionId: UUID? = null,
    val actualLiters: BigDecimal? = null,
)
data class CreateOperation(
    val fieldId: UUID, val farmId: UUID, val type: String,
    val plannedStart: Instant?, val plannedEnd: Instant?,
    val machineId: UUID?, val itemId: UUID?, val itemQuantity: BigDecimal?,
    val areaHa: BigDecimal? = null,
    val prescriptionId: UUID? = null,
)
data class CompleteOperation(val actualLiters: BigDecimal? = null)

@Service
class OperationService(
    private val repo: OperationJpaRepository,
    private val sagas: SagaJpaRepository,
    private val inventory: InventorySagaClient,
    private val agronomy: AgronomyPrescriptionClient,
    private val txManager: PlatformTransactionManager,
    private val fieldFarms: FieldFarmGuard,
    private val machineFarms: MachineFarmGuard,
    private val itemFarms: ItemFarmGuard,
    private val events: OperationEvents = NoOpOperationEvents,
) {
    private fun <T> inTx(block: () -> T): T = TransactionTemplate(txManager).execute { block() }!!

    fun list(scope: AccessScope, farmId: UUID? = null) =
        repo.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun machineSummary(scope: AccessScope, machineId: UUID, from: Instant, to: Instant): MachineWorkSummaryDto {
        machineFarms.requireRead(scope, machineId)
        requireRange(from, to)
        val ops = repo.findWorkInWindow(machineId, scope.resolveFarms(null), from, to)
            .map { op -> WorkSample(op.status, op.areaHa, op.itemId, op.itemQuantity, op.actualStart, op.plannedStart) }
        return OperationProgress.summarize(ops)
    }

    fun get(scope: AccessScope, id: UUID): OperationDto {
        val e = repo.findById(id).orElseThrow { NotFoundException("OPERATION_NOT_FOUND", "Not found") }
        scope.requireFarmRead(e.farmId, "OPERATION_NOT_FOUND", "Not found")
        return e.toDto()
    }

    private fun requireRange(from: Instant, to: Instant) {
        if (!to.isAfter(from) || java.time.Duration.between(from, to).toDays() > QueryLimits.MAX_TELEMETRY_DAYS) {
            throw DomainException("OPERATION_RANGE_EXCEEDED", "Range exceeds ${QueryLimits.MAX_TELEMETRY_DAYS} days")
        }
    }

    private fun requireAreaHa(area: BigDecimal?) {
        if (area == null) return
        if (area.signum() <= 0 || area > MAX_AREA_HA) {
            throw DomainException("OPERATION_AREA_INVALID", "areaHa must be positive and at most $MAX_AREA_HA")
        }
    }

    fun create(scope: AccessScope, cmd: CreateOperation): OperationDto {
        scope.requireFarm(cmd.farmId)
        fieldFarms.requireBelongsToFarm(cmd.fieldId, cmd.farmId)
        cmd.machineId?.let { machineFarms.requireBelongsToFarm(it, cmd.farmId) }
        cmd.itemId?.let { itemFarms.requireBelongsToFarm(it, cmd.farmId) }
        requireAreaHa(cmd.areaHa)
        // HTTP outside DB transaction
        cmd.prescriptionId?.let { rxId ->
            val rx = agronomy.get(rxId, cmd.farmId)
            if (rx.farmId != cmd.farmId || rx.fieldId != cmd.fieldId) {
                throw DomainException("PRESCRIPTION_FARM_MISMATCH", "Prescription farm/field does not match operation")
            }
        }
        return inTx {
            repo.save(
                OperationEntity(
                    UUID.randomUUID(), cmd.fieldId, cmd.farmId, cmd.type, "PLANNED",
                    cmd.plannedStart, cmd.plannedEnd, null, null, cmd.machineId, null, cmd.itemId, cmd.itemQuantity, cmd.areaHa,
                    cmd.prescriptionId, null,
                ),
            ).toDto()
        }
    }

    fun start(scope: AccessScope, id: UUID): OperationDto {
        val preview = inTx {
            val op = load(scope, id)
            if (op.status != "PLANNED" && op.status != "PAUSED" && op.status != "STARTING") {
                throw ConflictException("OPERATION_STATE_CONFLICT", "Operation cannot be started from ${op.status}")
            }
            StartCtx(op.status == "PLANNED", op.status, op.prescriptionId, op.farmId, op.fieldId)
        }
        if (preview.previousStatus == "STARTING") return resumeStart(scope, id)
        preview.prescriptionId?.let { requireApprovedPrescription(it, preview.farmId, preview.fieldId) }
        val saga = inTx {
            val op = load(scope, id)
            if (op.status != "PLANNED" && op.status != "PAUSED") {
                throw ConflictException("OPERATION_STATE_CONFLICT", "Operation cannot be started from ${op.status}")
            }
            val previous = op.status
            op.status = "STARTING"
            repo.save(op)
            sagas.save(
                SagaEntity(UUID.randomUUID(), op.id, "StartOperationSaga", "STARTED", previous, Instant.now(), Instant.now()),
            )
        }
        return finishStart(scope, id, saga, saga.payload ?: preview.previousStatus, preview.needsReserve)
    }

    private fun resumeStart(scope: AccessScope, id: UUID): OperationDto {
        val saga = inTx { sagas.findFirstByOperationIdAndTypeOrderByCreatedAtDesc(id, "StartOperationSaga") }
            ?: throw ConflictException("OPERATION_STATE_CONFLICT", "Operation start has no saga")
        val previous = saga.payload?.takeIf { it == "PLANNED" || it == "PAUSED" } ?: "PLANNED"
        if (saga.state == "COMPENSATION_FAILED") {
            try {
                if (previous == "PLANNED") {
                    inventoryMove(inTx { load(scope, id) }, "RELEASE", stepKey = "${saga.id}:UNDO-RESERVE")
                }
            } catch (ex: Exception) {
                throw ConflictException("SAGA_COMPENSATION_FAILED", ex.message ?: "Compensation failed")
            }
            inTx {
                forceStatus(scope, id, previous)
                saga.state = "COMPENSATED"
                saga.updatedAt = Instant.now()
                sagas.save(saga)
            }
            return start(scope, id)
        }
        return finishStart(scope, id, saga, previous, previous == "PLANNED")
    }

    private fun finishStart(
        scope: AccessScope,
        id: UUID,
        saga: SagaEntity,
        previous: String,
        needsReserve: Boolean,
    ): OperationDto {
        var reserveResult: InventoryStepResult? = null
        try {
            if (needsReserve) {
                reserveResult = inventoryMove(inTx { load(scope, id) }, "RESERVE", stepKey = "${saga.id}:RESERVE")
            }
            val started = inTx { persistStart(scope, id, saga) }
            try {
                events.operationStarted(started.id, started.farmId, started.fieldId, started.prescriptionId)
            } catch (_: Exception) {
                // Best-effort: broker failure must not fail or compensate the saga.
            }
            return started
        } catch (ex: Exception) {
            // A duplicate step belongs to the attempt that inserted it. Undoing it releases their stock.
            if (reserveResult == InventoryStepResult.AlreadyApplied) {
                throw ConflictException("SAGA_FAILED", ex.message ?: "Start still in progress")
            }
            val owned = reserveResult == InventoryStepResult.Applied
            val unknown = needsReserve && reserveResult == null && ex !is DomainException
            if (owned || unknown) {
                try {
                    inventoryMove(inTx { load(scope, id) }, "RELEASE", stepKey = "${saga.id}:UNDO-RESERVE")
                } catch (compensateEx: Exception) {
                    inTx {
                        saga.state = "COMPENSATION_FAILED"
                        saga.updatedAt = Instant.now()
                        sagas.save(saga)
                    }
                    throw ConflictException(
                        "SAGA_COMPENSATION_FAILED",
                        "${ex.message}; RELEASE failed: ${compensateEx.message}",
                    )
                }
            }
            inTx { forceStatus(scope, id, previous) }
            inTx {
                saga.state = "COMPENSATED"
                saga.updatedAt = Instant.now()
                sagas.save(saga)
            }
            if (ex is DomainException) throw ex
            throw ConflictException("SAGA_FAILED", ex.message ?: "Failed to start operation")
        }
    }

    private fun requireApprovedPrescription(prescriptionId: UUID, farmId: UUID, fieldId: UUID) {
        val rx = agronomy.get(prescriptionId, farmId)
        if (rx.farmId != farmId || rx.fieldId != fieldId) {
            throw DomainException("PRESCRIPTION_FARM_MISMATCH", "Prescription farm/field does not match operation")
        }
        if (rx.status != "APPROVED") {
            throw ConflictException("PRESCRIPTION_NOT_APPROVED", "Prescription must be APPROVED to start")
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

    fun complete(scope: AccessScope, id: UUID, cmd: CompleteOperation = CompleteOperation()): OperationDto {
        val preview = inTx {
            val op = load(scope, id)
            if (op.status != "IN_PROGRESS" && op.status != "PAUSED" && op.status != "COMPLETING") {
                throw ConflictException("OPERATION_STATE_CONFLICT", "Cannot complete from ${op.status}")
            }
            op.status to op.itemQuantity
        }
        val liters = requireActualLiters(cmd.actualLiters, preview.second)
        if (preview.first == "COMPLETING") return resumeComplete(scope, id, liters)
        val saga = inTx {
            val op = load(scope, id)
            if (op.status != "IN_PROGRESS" && op.status != "PAUSED") {
                throw ConflictException("OPERATION_STATE_CONFLICT", "Cannot complete from ${op.status}")
            }
            val previous = op.status
            op.status = "COMPLETING"
            repo.save(op)
            sagas.save(
                SagaEntity(UUID.randomUUID(), op.id, "CompleteOperationSaga", "STARTED", previous, Instant.now(), Instant.now()),
            )
        }
        return finishComplete(scope, id, saga, saga.payload ?: preview.first, liters)
    }

    private fun resumeComplete(scope: AccessScope, id: UUID, liters: BigDecimal?): OperationDto {
        val saga = inTx { sagas.findFirstByOperationIdAndTypeOrderByCreatedAtDesc(id, "CompleteOperationSaga") }
            ?: throw ConflictException("OPERATION_STATE_CONFLICT", "Operation complete has no saga")
        val previous = saga.payload?.substringBefore("|")?.takeIf { it == "IN_PROGRESS" || it == "PAUSED" } ?: "IN_PROGRESS"
        if (saga.state == "COMPENSATION_FAILED") {
            try {
                restoreComplete(scope, id, saga, liters)
            } catch (ex: Exception) {
                throw ConflictException("SAGA_COMPENSATION_FAILED", ex.message ?: "Compensation failed")
            }
            inTx {
                forceStatus(scope, id, previous)
                saga.state = "COMPENSATED"
                saga.updatedAt = Instant.now()
                sagas.save(saga)
            }
            return complete(scope, id, CompleteOperation(liters))
        }
        return finishComplete(scope, id, saga, previous, liters)
    }

    private fun finishComplete(
        scope: AccessScope,
        id: UUID,
        saga: SagaEntity,
        previous: String,
        liters: BigDecimal?,
    ): OperationDto {
        var consumeResult: InventoryStepResult? = null
        val planned = inTx { load(scope, id).itemQuantity }
        val unused = unusedReservation(planned, liters)
        try {
            consumeResult = inventoryMove(inTx { load(scope, id) }, "CONSUME", liters, "${saga.id}:CONSUME")
            if (consumeResult != InventoryStepResult.Skipped && unused != null) {
                inventoryMove(inTx { load(scope, id) }, "RELEASE", unused, "${saga.id}:RELEASE-UNUSED")
            }
            return inTx { persistComplete(scope, id, liters, saga) }
        } catch (ex: Exception) {
            if (consumeResult == InventoryStepResult.AlreadyApplied) {
                throw ConflictException("SAGA_FAILED", ex.message ?: "Complete still in progress")
            }
            val owned = consumeResult == InventoryStepResult.Applied
            val unknown = consumeResult == null && ex !is DomainException
            if (owned || unknown) {
                try {
                    restoreComplete(scope, id, saga, liters)
                } catch (compensateEx: Exception) {
                    inTx {
                        saga.state = "COMPENSATION_FAILED"
                        saga.updatedAt = Instant.now()
                        sagas.save(saga)
                    }
                    throw ConflictException(
                        "SAGA_COMPENSATION_FAILED",
                        "${ex.message}; restore failed: ${compensateEx.message}",
                    )
                }
            }
            inTx { forceStatus(scope, id, previous) }
            inTx {
                saga.state = "COMPENSATED"
                saga.updatedAt = Instant.now()
                sagas.save(saga)
            }
            if (ex is DomainException) throw ex
            throw ConflictException("SAGA_FAILED", ex.message ?: "Failed to complete operation")
        }
    }

    private fun restoreComplete(scope: AccessScope, id: UUID, saga: SagaEntity, liters: BigDecimal?) {
        val op = inTx { load(scope, id) }
        val unused = unusedReservation(op.itemQuantity, liters)
        inventoryMove(op, "IN", liters, "${saga.id}:UNDO-CONSUME")
        if (unused != null) {
            inventoryMove(op, "RELEASE", unused, "${saga.id}:RELEASE-UNUSED")
        }
        inventoryMove(op, "RESERVE", stepKey = "${saga.id}:RESTORE-RESERVE")
    }

    private fun requireActualLiters(value: BigDecimal?, planned: BigDecimal?): BigDecimal? {
        if (value == null) return null
        val cap = when {
            planned != null && planned.signum() > 0 && planned < MAX_ACTUAL_LITERS -> planned
            else -> MAX_ACTUAL_LITERS
        }
        if (value.signum() <= 0 || value > cap) {
            throw DomainException(
                "ACTUAL_LITERS_INVALID",
                "actualLiters must be positive and at most the reserved quantity",
            )
        }
        return value
    }

    private fun unusedReservation(planned: BigDecimal?, actual: BigDecimal?): BigDecimal? {
        if (planned == null || actual == null || planned <= actual) return null
        return planned.subtract(actual)
    }

    private fun persistStart(scope: AccessScope, id: UUID, saga: SagaEntity): OperationDto {
        val op = load(scope, id)
        if (op.status != "STARTING") {
            throw ConflictException("OPERATION_STATE_CONFLICT", "Operation cannot be started from ${op.status}")
        }
        op.status = "IN_PROGRESS"
        op.actualStart = op.actualStart ?: Instant.now()
        op.pauseReason = null
        saga.state = "COMPLETED"
        saga.updatedAt = Instant.now()
        sagas.save(saga)
        return repo.save(op).toDto()
    }

    private fun persistComplete(scope: AccessScope, id: UUID, liters: BigDecimal?, saga: SagaEntity): OperationDto {
        val op = load(scope, id)
        if (op.status != "COMPLETING") {
            throw ConflictException("OPERATION_STATE_CONFLICT", "Cannot complete from ${op.status}")
        }
        op.status = "COMPLETED"
        if (liters != null) op.actualLiters = liters
        op.actualEnd = Instant.now()
        saga.state = "COMPLETED"
        saga.updatedAt = Instant.now()
        sagas.save(saga)
        return repo.save(op).toDto()
    }

    private fun forceStatus(scope: AccessScope, id: UUID, previous: String) {
        val op = load(scope, id)
        if (op.status != "STARTING" && op.status != "COMPLETING") return
        op.status = previous
        repo.save(op)
    }

    private fun inventoryMove(
        op: OperationEntity,
        type: String,
        consumeLiters: BigDecimal? = null,
        stepKey: String,
    ): InventoryStepResult {
        val itemId = op.itemId ?: return InventoryStepResult.Skipped
        val qty = consumeQuantity(op, type, consumeLiters) ?: return InventoryStepResult.Skipped
        return inventory.move(itemId, type, qty, op.id.toString(), op.farmId, stepKey)
    }

    /**
     * CONSUME and the compensating IN use the liters of this attempt when present.
     * RESERVE and RELEASE always use the planned item quantity.
     */
    private fun consumeQuantity(op: OperationEntity, type: String, consumeLiters: BigDecimal?): BigDecimal? {
        if ((type == "RELEASE" || type == "RESERVE") && consumeLiters != null) return consumeLiters
        if (type == "CONSUME" || type == "IN") {
            val actual = consumeLiters ?: op.actualLiters
            if (actual != null && actual.signum() > 0) return actual
        }
        return op.itemQuantity
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
            val status: String, val machine: String?, val item: String, val offsetDays: Long = 1,
            val prescriptionKey: String? = null,
        )
        val now = Instant.now()
        val rows = listOf(
            Row("op-001", "field-001", "farm-001", "PLANTING", "COMPLETED", "machine-001", "item-001"),
            // op-002 is field-001 SPRAYING — linked to rx-spot-001 for investor spray demo
            Row("op-002", "field-001", "farm-001", "SPRAYING", "IN_PROGRESS", "machine-002", "item-001", prescriptionKey = "rx-spot-001"),
            Row("op-003", "field-002", "farm-001", "FERTILIZING", "PLANNED", "machine-001", "item-002"),
            Row("op-004", "field-003", "farm-001", "INSPECTION", "PLANNED", null, "item-001"),
            Row("op-005", "field-004", "farm-002", "PLANTING", "PAUSED", "machine-004", "item-003"),
            Row("op-006", "field-005", "farm-002", "SPRAYING", "IN_PROGRESS", "machine-006", "item-005"),
            Row("op-007", "field-006", "farm-003", "PLANTING", "COMPLETED", "machine-005", "item-007"),
            Row("op-008", "field-007", "farm-003", "FERTILIZING", "PAUSED", "machine-005", "item-008"),
            Row("op-009", "field-008", "farm-003", "SPRAYING", "PLANNED", "machine-013", "item-008"),
            Row("op-010", "field-009", "farm-004", "PLANTING", "IN_PROGRESS", "machine-007", "item-009"),
            Row("op-011", "field-010", "farm-004", "HARVEST", "PLANNED", "machine-008", "item-010"),
            Row("op-012", "field-011", "farm-004", "FERTILIZING", "COMPLETED", "machine-007", "item-009"),
            Row("op-013", "field-012", "farm-005", "PLANTING", "PLANNED", "machine-009", "item-011"),
            Row("op-014", "field-013", "farm-005", "SPRAYING", "PAUSED", "machine-009", "item-011"),
            Row("op-015", "field-016", "farm-005", "INSPECTION", "COMPLETED", null, "item-012"),
            Row("op-016", "field-017", "farm-006", "PLANTING", "IN_PROGRESS", "machine-010", "item-013"),
            Row("op-017", "field-018", "farm-006", "FERTILIZING", "PLANNED", "machine-010", "item-014"),
            Row("op-018", "field-019", "farm-007", "SPRAYING", "PAUSED", "machine-011", "item-015"),
            Row("op-019", "field-020", "farm-007", "PLANTING", "COMPLETED", null, "item-015"),
            Row("op-020", "field-021", "farm-008", "FERTILIZING", "IN_PROGRESS", "machine-012", "item-016"),
            Row("op-021", "field-014", "farm-001", "HARVEST", "COMPLETED", "machine-003", "item-004", 6),
            Row("op-022", "field-018", "farm-006", "SPRAYING", "IN_PROGRESS", "machine-014", "item-014", 1),
            Row("op-023", "field-001", "farm-001", "SPRAYING", "COMPLETED", "machine-001", "item-001", 5),
            Row("op-024", "field-002", "farm-001", "FERTILIZING", "COMPLETED", "machine-002", "item-002", 4),
            Row("op-025", "field-015", "farm-002", "PLANTING", "COMPLETED", "machine-004", "item-003", 3),
            Row("op-026", "field-006", "farm-003", "PLANTING", "IN_PROGRESS", "machine-005", "item-007", 0),
            Row("op-027", "field-005", "farm-002", "SPRAYING", "PAUSED", "machine-006", "item-005", 2),
            Row("op-028", "field-009", "farm-004", "FERTILIZING", "COMPLETED", "machine-007", "item-009", 6),
            Row("op-029", "field-010", "farm-004", "HARVEST", "COMPLETED", "machine-008", "item-010", 2),
            Row("op-030", "field-012", "farm-005", "FERTILIZING", "COMPLETED", "machine-009", "item-011", 4),
            Row("op-031", "field-017", "farm-006", "PLANTING", "COMPLETED", "machine-010", "item-013", 5),
            Row("op-032", "field-019", "farm-007", "SPRAYING", "IN_PROGRESS", "machine-011", "item-015", 1),
            Row("op-033", "field-022", "farm-008", "INSPECTION", "COMPLETED", "machine-012", "item-016", 3),
            Row("op-034", "field-008", "farm-003", "SPRAYING", "COMPLETED", "machine-013", "item-008", 2),
            // Insert-missing PLANNED op on field-001 linked to rx-draft-001 (start-failure demo)
            Row("op-rx-draft", "field-001", "farm-001", "SPRAYING", "PLANNED", "machine-002", "item-001", prescriptionKey = "rx-draft-001"),
        )
        val existing = repo.findAllById(rows.map { DemoIds.uuid(it.key) }).associateBy { it.id }
        repo.saveAll(
            rows.map { r ->
                val id = DemoIds.uuid(r.key)
                val area = FIELD_AREA[r.field]
                val found = existing[id]
                val plannedStart = now.minus(r.offsetDays.coerceAtLeast(1), ChronoUnit.DAYS)
                val plannedEnd = now.plus(1, ChronoUnit.DAYS)
                val actualStart = if (r.status != "PLANNED") now.minus(r.offsetDays, ChronoUnit.DAYS) else null
                val actualEnd =
                    if (r.status == "COMPLETED") now.minus(r.offsetDays, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS)
                    else null
                val rxId = r.prescriptionKey?.let { DemoIds.uuid(it) }
                if (found != null) {
                    found.fieldId = DemoIds.uuid(r.field)
                    found.farmId = DemoIds.uuid(r.farm)
                    found.type = r.type
                    found.itemId = DemoIds.uuid(r.item)
                    if (found.machineId == null) found.machineId = r.machine?.let { DemoIds.uuid(it) }
                    found.areaHa = area
                    found.plannedStart = plannedStart
                    found.plannedEnd = plannedEnd
                    found.actualStart = actualStart
                    found.actualEnd = actualEnd
                    // Do not reset status; only set prescription_id if null
                    if (found.prescriptionId == null && rxId != null) found.prescriptionId = rxId
                    found
                } else {
                    OperationEntity(
                        id, DemoIds.uuid(r.field), DemoIds.uuid(r.farm), r.type, r.status,
                        plannedStart, plannedEnd, actualStart, actualEnd,
                        r.machine?.let { DemoIds.uuid(it) },
                        if (r.status == "PAUSED") "RAIN" else null,
                        DemoIds.uuid(r.item),
                        BigDecimal("20"),
                        area,
                        rxId,
                        null,
                    )
                }
            },
        )
    }

    private fun OperationEntity.toDto() = OperationDto(
        id, fieldId, farmId, type, status, plannedStart, plannedEnd, actualStart, actualEnd,
        machineId, pauseReason, itemId, itemQuantity, areaHa, prescriptionId, actualLiters,
    )

    private data class StartCtx(
        val needsReserve: Boolean,
        val previousStatus: String,
        val prescriptionId: UUID?,
        val farmId: UUID,
        val fieldId: UUID,
    )

    private companion object {
        val MAX_AREA_HA = BigDecimal("100000")
        val MAX_ACTUAL_LITERS = BigDecimal("100000")
        val FIELD_AREA = mapOf(
            "field-001" to BigDecimal("120.5"),
            "field-002" to BigDecimal("95.0"),
            "field-003" to BigDecimal("80.0"),
            "field-004" to BigDecimal("210.0"),
            "field-005" to BigDecimal("175.0"),
            "field-006" to BigDecimal("320.0"),
            "field-007" to BigDecimal("280.0"),
            "field-008" to BigDecimal("190.0"),
            "field-009" to BigDecimal("150.0"),
            "field-010" to BigDecimal("140.0"),
            "field-011" to BigDecimal("110.0"),
            "field-012" to BigDecimal("95.0"),
            "field-013" to BigDecimal("88.0"),
            "field-014" to BigDecimal("70.0"),
            "field-015" to BigDecimal("130.0"),
            "field-016" to BigDecimal("102.0"),
            "field-017" to BigDecimal("155.0"),
            "field-018" to BigDecimal("140.0"),
            "field-019" to BigDecimal("125.0"),
            "field-020" to BigDecimal("118.0"),
            "field-021" to BigDecimal("105.0"),
            "field-022" to BigDecimal("98.0"),
        )
    }
}

@Service
class OperationSeed(
    private val svc: OperationService,
    private val gate: com.precisionfarming.security.DemoSeedGate,
) {
    @Bean
    fun seedOps() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
