package com.precisionfarming.alert.application

import com.precisionfarming.alert.application.demo.DemoAlerts
import com.precisionfarming.alert.infrastructure.AlertEntity
import com.precisionfarming.alert.infrastructure.AlertJpaRepository
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class AlertDto(
    val id: UUID, val farmId: UUID, val severity: String, val type: String, val title: String,
    val message: String, val entityType: String?, val entityId: UUID?, val status: String, val createdAt: Instant,
)

@Service
class AlertService(private val repo: AlertJpaRepository) {
    fun list(scope: AccessScope, farmId: UUID? = null) =
        repo.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }
    @Transactional
    fun ack(scope: AccessScope, id: UUID): AlertDto {
        val e = repo.findById(id).orElseThrow { NotFoundException("ALERT_NOT_FOUND", "Alert not found") }
        scope.requireEntityFarm(e.farmId)
        e.status = "ACKED"
        return repo.save(e).toDto()
    }
    @Transactional
    fun seed() {
        repo.saveAll(DemoAlerts.rows())
    }
    private fun AlertEntity.toDto() = AlertDto(id, farmId, severity, type, title, message, entityType, entityId, status, createdAt)
}

@Service
class AlertSeed(private val svc: AlertService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedAlerts() = ApplicationRunner { if (seed) svc.seed() }
}
