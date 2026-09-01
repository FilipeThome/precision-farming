package com.precisionfarming.alert.application

import com.precisionfarming.alert.infrastructure.AlertEntity
import com.precisionfarming.alert.infrastructure.AlertJpaRepository
import com.precisionfarming.common.DemoIds
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
        if (repo.existsById(DemoIds.uuid("alert-001"))) return
        repo.saveAll(
            listOf(
                AlertEntity(DemoIds.uuid("alert-001"), DemoIds.uuid("farm-001"), "CRITICAL", "MACHINE", "Temperatura alta", "Trator 01 com variação de temperatura", "MACHINE", DemoIds.uuid("machine-001"), "OPEN", Instant.now()),
                AlertEntity(DemoIds.uuid("alert-002"), DemoIds.uuid("farm-001"), "WARNING", "WEATHER", "Janela de pulverização", "Chuva prevista, pulverização desfavorável", "FARM", DemoIds.uuid("farm-001"), "OPEN", Instant.now()),
                AlertEntity(DemoIds.uuid("alert-003"), DemoIds.uuid("farm-002"), "INFO", "OPERATION", "Operação pausada", "Plantio no Talhão Norte pausado", "OPERATION", DemoIds.uuid("op-005"), "OPEN", Instant.now()),
            ),
        )
    }
    private fun AlertEntity.toDto() = AlertDto(id, farmId, severity, type, title, message, entityType, entityId, status, createdAt)
}

@Service
class AlertSeed(private val svc: AlertService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedAlerts() = ApplicationRunner { if (seed) svc.seed() }
}
