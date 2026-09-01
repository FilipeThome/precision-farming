package com.precisionfarming.security

import com.precisionfarming.common.DemoIds
import java.util.UUID

object DemoTenant {
    val ID: UUID = DemoIds.uuid("tenant-demo")
}

object DemoFarmDirectory {
    val ALL: Set<UUID> = (1..8).map { DemoIds.uuid("farm-%03d".format(it)) }.toSet()

    fun forRole(role: String): Set<UUID> = when (role.uppercase()) {
        "ADMIN" -> ALL
        "FARM_MANAGER", "MAINTENANCE" -> setOf(
            DemoIds.uuid("farm-001"),
            DemoIds.uuid("farm-002"),
            DemoIds.uuid("farm-003"),
        )
        else -> setOf(DemoIds.uuid("farm-001"))
    }
}
