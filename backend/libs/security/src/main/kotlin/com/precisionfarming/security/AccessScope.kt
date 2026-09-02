package com.precisionfarming.security

import com.precisionfarming.common.ForbiddenException
import java.util.UUID

data class AccessScope(
    val tenantId: UUID,
    val farmIds: Set<UUID>,
    val role: String,
    val userId: UUID? = null,
) {
    fun resolveFarms(requested: UUID?): Set<UUID> {
        if (requested != null) {
            requireFarm(requested)
            return setOf(requested)
        }
        return farmIds
    }

    fun requireFarm(farmId: UUID) {
        if (farmId !in farmIds) {
            throw ForbiddenException("Farm out of scope", "FARM_SCOPE_DENIED")
        }
    }

    fun requireEntityFarm(farmId: UUID) = requireFarm(farmId)

    fun requireUserId(): UUID =
        userId ?: throw ForbiddenException("Missing subject", "SUBJECT_REQUIRED")
}
