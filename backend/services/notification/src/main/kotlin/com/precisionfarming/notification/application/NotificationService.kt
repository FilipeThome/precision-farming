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

@Service
class NotificationService(private val repo: NotificationJpaRepository) {
    fun list() = repo.findAll()
    @Transactional
    fun seed() {
        if (repo.existsById(DemoIds.uuid("notif-001"))) return
        repo.save(
            NotificationEntity(
                DemoIds.uuid("notif-001"), DemoIds.uuid("manager@precisionfarming.demo"),
                "ALERT", "Alerta crítico", "Trator 01 com risco de falha (modelo demo)", null, Instant.now(),
            ),
        )
    }
}

@Service
class NotificationSeed(private val svc: NotificationService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedN() = ApplicationRunner { if (seed) svc.seed() }
}
