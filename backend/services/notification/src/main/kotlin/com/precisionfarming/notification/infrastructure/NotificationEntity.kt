package com.precisionfarming.notification.infrastructure

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id val id: UUID,
    @Column(name = "user_id") val userId: UUID,
    val type: String,
    val title: String,
    val body: String,
    @Column(name = "read_at") var readAt: Instant?,
    @Column(name = "created_at") val createdAt: Instant,
)
interface NotificationJpaRepository : JpaRepository<NotificationEntity, UUID>
