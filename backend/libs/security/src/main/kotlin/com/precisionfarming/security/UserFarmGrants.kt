package com.precisionfarming.security

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** In-process grants so a newly created farm is in the creator's scope before the next JWT. */
object UserFarmGrants {
    private val byUser = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    fun grant(userId: UUID, farmId: UUID) {
        byUser.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(farmId)
    }

    fun revoke(farmId: UUID, userId: UUID? = null) {
        if (userId != null) {
            byUser[userId]?.remove(farmId)
            return
        }
        byUser.values.forEach { it.remove(farmId) }
    }

    fun farmIds(userId: UUID): Set<UUID> = byUser[userId]?.toSet().orEmpty()
}
