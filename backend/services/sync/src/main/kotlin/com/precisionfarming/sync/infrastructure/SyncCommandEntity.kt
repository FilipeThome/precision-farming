package com.precisionfarming.sync.infrastructure

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sync_commands")
class SyncCommandEntity(
    @Id val id: UUID,
    @Column(name = "device_id") val deviceId: String,
    @Column(name = "client_operation_id") val clientOperationId: String,
    @Column(name = "command_type") val commandType: String,
    val payload: String,
    var status: String,
    @Column(name = "created_at") val createdAt: Instant,
)
interface SyncJpaRepository : JpaRepository<SyncCommandEntity, UUID> {
    fun existsByClientOperationId(clientOperationId: String): Boolean
    fun findByClientOperationIdIn(ids: Collection<String>): List<SyncCommandEntity>
    fun findByDeviceIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(deviceId: String, createdAt: Instant): List<SyncCommandEntity>
}
