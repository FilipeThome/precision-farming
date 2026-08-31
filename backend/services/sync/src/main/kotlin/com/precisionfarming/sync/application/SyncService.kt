package com.precisionfarming.sync.application

import com.precisionfarming.sync.api.PushRequest
import com.precisionfarming.sync.infrastructure.SyncCommandEntity
import com.precisionfarming.sync.infrastructure.SyncJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SyncService(private val repo: SyncJpaRepository) {
    @Transactional
    fun push(body: PushRequest): Map<String, Any> {
        val ids = body.commands.map { it.clientOperationId }
        val existing = if (ids.isEmpty()) {
            HashSet()
        } else {
            repo.findByClientOperationIdIn(ids).map { it.clientOperationId }.toHashSet()
        }
        val toSave = ArrayList<SyncCommandEntity>(body.commands.size)
        val applied = body.commands.map { cmd ->
            if (cmd.clientOperationId in existing) {
                mapOf("clientOperationId" to cmd.clientOperationId, "status" to "DUPLICATE")
            } else {
                existing.add(cmd.clientOperationId)
                toSave.add(
                    SyncCommandEntity(
                        UUID.randomUUID(), body.deviceId, cmd.clientOperationId, cmd.type,
                        cmd.payload.toString(), "APPLIED", cmd.createdAt,
                    ),
                )
                mapOf("clientOperationId" to cmd.clientOperationId, "status" to "APPLIED")
            }
        }
        if (toSave.isNotEmpty()) repo.saveAll(toSave)
        return mapOf("results" to applied)
    }

    fun pull(deviceId: String, cursor: String?) = mapOf(
        "commands" to emptyList<Any>(),
        "cursor" to Instant.now().toString(),
        "deviceId" to deviceId,
    )
}
