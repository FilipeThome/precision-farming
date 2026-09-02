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
    var type: String,
    var title: String,
    var body: String,
    @Column(name = "read_at") var readAt: Instant?,
    @Column(name = "created_at") val createdAt: Instant,
)
interface NotificationJpaRepository : JpaRepository<NotificationEntity, UUID> {
    fun findByUserId(userId: UUID): List<NotificationEntity>
}
