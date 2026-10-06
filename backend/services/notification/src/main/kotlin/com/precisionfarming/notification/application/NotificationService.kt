package com.precisionfarming.notification.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.notification.infrastructure.NotificationEntity
import com.precisionfarming.notification.infrastructure.NotificationJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class NotificationDto(
    val id: UUID,
    val userId: UUID,
    val type: String,
    val title: String,
    val body: String,
    val readAt: Instant?,
    val createdAt: Instant,
)

@Service
class NotificationService(private val repo: NotificationJpaRepository) {
    fun list(userId: UUID): List<NotificationDto> =
        repo.findByUserId(userId).map { it.toDto() }

    @Transactional
    fun seed() {
        repo.save(
            NotificationEntity(
                DemoIds.uuid("notif-001"), DemoIds.uuid("manager@precisionfarming.demo"),
                "ALERT", "CRITICAL", "MACHINE_FAILURE_RISK", null, Instant.now(),
            ),
        )
    }

    private fun NotificationEntity.toDto() = NotificationDto(id, userId, type, title, body, readAt, createdAt)
}

@Service
class NotificationSeed(private val svc: NotificationService, private val gate: com.precisionfarming.security.DemoSeedGate) {
    @Bean fun seedN() = ApplicationRunner { if (gate.permits()) svc.seed() }
}
