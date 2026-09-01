package com.precisionfarming.security

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.ForbiddenException
import java.util.UUID

/** Demo seed machine → farm mapping for telemetry/AI when no farm column exists. */
object DemoMachineFarms {
    private val BY_MACHINE: Map<UUID, UUID> = mapOf(
        DemoIds.uuid("machine-001") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("machine-002") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("machine-003") to DemoIds.uuid("farm-001"),
        DemoIds.uuid("machine-004") to DemoIds.uuid("farm-002"),
        DemoIds.uuid("machine-005") to DemoIds.uuid("farm-003"),
    )

    fun farmId(machineId: UUID): UUID? = BY_MACHINE[machineId]

    fun requireMachine(scope: AccessScope, machineId: UUID) {
        val farmId = farmId(machineId)
            ?: throw ForbiddenException("Machine out of scope", "FARM_SCOPE_DENIED")
        scope.requireFarm(farmId)
    }
}
