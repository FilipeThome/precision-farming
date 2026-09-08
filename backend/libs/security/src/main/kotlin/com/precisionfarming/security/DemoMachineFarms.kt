package com.precisionfarming.security

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Demo seed machine → farm mapping, plus runtime registrations for machines
 * created via API (seed map is never removed).
 */
object DemoMachineFarms {
    private val SEED: Map<UUID, UUID> = mapOf(
        DemoIds.uuid("machine-001") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("machine-002") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("machine-003") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("machine-004") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("machine-005") to DemoIds.uuid("farm-003"),
        DemoIds.uuid("machine-006") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("machine-007") to DemoIds.uuid("farm-004"),
        DemoIds.uuid("machine-008") to DemoIds.uuid("farm-004"),
        DemoIds.uuid("machine-009") to DemoIds.uuid("farm-005"),
        DemoIds.uuid("machine-010") to DemoIds.uuid("farm-006"),
        DemoIds.uuid("machine-011") to DemoIds.uuid("farm-007"),
        DemoIds.uuid("machine-012") to DemoIds.uuid("farm-008"),
        DemoIds.uuid("machine-013") to DemoIds.uuid("farm-003"),
        DemoIds.uuid("machine-014") to DemoIds.uuid("farm-006"),
    )

    private val runtime = ConcurrentHashMap<UUID, UUID>()

    fun register(machineId: UUID, farmId: UUID) {
        runtime[machineId] = farmId
    }

    fun farmId(machineId: UUID): UUID? = runtime[machineId] ?: SEED[machineId]

    fun requireMachine(scope: AccessScope, machineId: UUID) {
        val farmId = farmId(machineId)
            ?: throw ForbiddenException("Machine out of scope", "FARM_SCOPE_DENIED")
        scope.requireFarm(farmId)
    }

    fun requireMachineRead(scope: AccessScope, machineId: UUID) {
        val farmId = farmId(machineId)
            ?: throw NotFoundException("MACHINE_NOT_FOUND", "Not found")
        scope.requireFarmRead(farmId, "MACHINE_NOT_FOUND", "Not found")
    }

    fun requireBelongsToFarm(machineId: UUID, farmId: UUID) {
        val mapped = farmId(machineId)
            ?: throw ForbiddenException("Machine out of scope", "FARM_SCOPE_DENIED")
        if (mapped != farmId) {
            throw ForbiddenException("Machine does not belong to farm", "FARM_SCOPE_DENIED")
        }
    }
}

/** Demo seed field → farm mapping, plus runtime registrations for API-created fields. */
object DemoFieldFarms {
    private val SEED: Map<UUID, UUID> = mapOf(
        DemoIds.uuid("field-001") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("field-002") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("field-003") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("field-004") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("field-005") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("field-006") to DemoIds.uuid("farm-003"),
        DemoIds.uuid("field-007") to DemoIds.uuid("farm-003"),
        DemoIds.uuid("field-008") to DemoIds.uuid("farm-003"),
        DemoIds.uuid("field-009") to DemoIds.uuid("farm-004"),
        DemoIds.uuid("field-010") to DemoIds.uuid("farm-004"),
        DemoIds.uuid("field-011") to DemoIds.uuid("farm-004"),
        DemoIds.uuid("field-012") to DemoIds.uuid("farm-005"),
        DemoIds.uuid("field-013") to DemoIds.uuid("farm-005"),
        DemoIds.uuid("field-014") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("field-015") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("field-016") to DemoIds.uuid("farm-005"),
        DemoIds.uuid("field-017") to DemoIds.uuid("farm-006"),
        DemoIds.uuid("field-018") to DemoIds.uuid("farm-006"),
        DemoIds.uuid("field-019") to DemoIds.uuid("farm-007"),
        DemoIds.uuid("field-020") to DemoIds.uuid("farm-007"),
        DemoIds.uuid("field-021") to DemoIds.uuid("farm-008"),
        DemoIds.uuid("field-022") to DemoIds.uuid("farm-008"),
    )

    private val runtime = ConcurrentHashMap<UUID, UUID>()

    fun register(fieldId: UUID, farmId: UUID) {
        runtime[fieldId] = farmId
    }

    fun farmId(fieldId: UUID): UUID? = runtime[fieldId] ?: SEED[fieldId]

    fun requireField(scope: AccessScope, fieldId: UUID) {
        val farmId = farmId(fieldId)
            ?: throw ForbiddenException("Field out of scope", "FARM_SCOPE_DENIED")
        scope.requireFarm(farmId)
    }

    fun requireBelongsToFarm(fieldId: UUID, farmId: UUID) {
        val mapped = farmId(fieldId)
            ?: throw ForbiddenException("Field out of scope", "FARM_SCOPE_DENIED")
        if (mapped != farmId) {
            throw ForbiddenException("Field does not belong to farm", "FARM_SCOPE_DENIED")
        }
    }
}

/** Demo seed inventory item → farm mapping, plus runtime registrations for API-created items. */
object DemoItemFarms {
    private val SEED: Map<UUID, UUID> = mapOf(
        DemoIds.uuid("item-001") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("item-002") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("item-003") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("item-004") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("item-005") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("item-006") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("item-007") to DemoIds.uuid("farm-003"),
        DemoIds.uuid("item-008") to DemoIds.uuid("farm-003"),
        DemoIds.uuid("item-009") to DemoIds.uuid("farm-004"),
        DemoIds.uuid("item-010") to DemoIds.uuid("farm-004"),
        DemoIds.uuid("item-011") to DemoIds.uuid("farm-005"),
        DemoIds.uuid("item-012") to DemoIds.uuid("farm-005"),
        DemoIds.uuid("item-013") to DemoIds.uuid("farm-006"),
        DemoIds.uuid("item-014") to DemoIds.uuid("farm-006"),
        DemoIds.uuid("item-015") to DemoIds.uuid("farm-007"),
        DemoIds.uuid("item-016") to DemoIds.uuid("farm-008"),
    )

    private val runtime = ConcurrentHashMap<UUID, UUID>()

    fun register(itemId: UUID, farmId: UUID) {
        runtime[itemId] = farmId
    }

    fun farmId(itemId: UUID): UUID? = runtime[itemId] ?: SEED[itemId]

    fun requireBelongsToFarm(itemId: UUID, farmId: UUID) {
        val mapped = farmId(itemId)
            ?: throw ForbiddenException("Item out of scope", "FARM_SCOPE_DENIED")
        if (mapped != farmId) {
            throw ForbiddenException("Item does not belong to farm", "FARM_SCOPE_DENIED")
        }
    }
}
