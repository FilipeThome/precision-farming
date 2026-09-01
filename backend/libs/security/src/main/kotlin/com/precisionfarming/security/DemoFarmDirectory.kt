package com.precisionfarming.security

import com.precisionfarming.common.DemoIds
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object DemoTenant {
    val ID: UUID = DemoIds.uuid("tenant-demo")
}

object DemoFarmDirectory {
    val ALL: Set<UUID> = (1..8).map { DemoIds.uuid("farm-%03d".format(it)) }.toSet()

    /** Farms created at runtime (seed farms stay in [ALL]; never removed). */
    private val runtime = ConcurrentHashMap.newKeySet<UUID>()

    fun register(farmId: UUID) {
        runtime.add(farmId)
    }

    fun unregister(farmId: UUID) {
        runtime.remove(farmId)
    }

    fun forRole(role: String): Set<UUID> = when (role.uppercase()) {
        "ADMIN" -> ALL + runtime
        "FARM_MANAGER", "MAINTENANCE" -> setOf(
            DemoIds.uuid("farm-001"),
            DemoIds.uuid("farm-002"),
            DemoIds.uuid("farm-003"),
        ) + runtime
        else -> setOf(DemoIds.uuid("farm-001"))
    }
}
