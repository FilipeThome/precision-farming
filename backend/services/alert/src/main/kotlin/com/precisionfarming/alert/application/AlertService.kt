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
        val now = Instant.now()
        val rows = listOf(
            AlertEntity(DemoIds.uuid("alert-001"), DemoIds.uuid("farm-001"), "CRITICAL", "MACHINE", "Temperatura alta", "Trator 01 com variação de temperatura", "MACHINE", DemoIds.uuid("machine-001"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-002"), DemoIds.uuid("farm-001"), "WARNING", "WEATHER", "Janela de pulverização", "Chuva prevista, pulverização desfavorável", "FARM", DemoIds.uuid("farm-001"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-003"), DemoIds.uuid("farm-002"), "INFO", "OPERATION", "Operação pausada", "Plantio no Talhão Norte pausado", "OPERATION", DemoIds.uuid("op-005"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-004"), DemoIds.uuid("farm-003"), "CRITICAL", "MACHINE", "Pressão hidráulica", "Plantadeira 01 com queda de pressão", "MACHINE", DemoIds.uuid("machine-005"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-005"), DemoIds.uuid("farm-004"), "CRITICAL", "OPERATION", "Atraso crítico", "Plantio Leste fora da janela", "OPERATION", DemoIds.uuid("op-010"), "ACKED", now),
            AlertEntity(DemoIds.uuid("alert-006"), DemoIds.uuid("farm-002"), "WARNING", "MACHINE", "Manutenção vencida", "Trator 02 com WO em atraso", "MACHINE", DemoIds.uuid("machine-004"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-007"), DemoIds.uuid("farm-005"), "WARNING", "FIELD", "Umidade baixa", "Talhão 2 com déficit hídrico", "FIELD", DemoIds.uuid("field-013"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-008"), DemoIds.uuid("farm-006"), "INFO", "OPERATION", "Plantio em andamento", "Talhão VV-01 em operação", "OPERATION", DemoIds.uuid("op-016"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-009"), DemoIds.uuid("farm-007"), "WARNING", "MACHINE", "Pulverizador parado", "Máquina em manutenção", "MACHINE", DemoIds.uuid("machine-011"), "ACKED", now),
            AlertEntity(DemoIds.uuid("alert-010"), DemoIds.uuid("farm-008"), "INFO", "WEATHER", "Janela favorável", "Próximas 24h boas para adubação", "FARM", DemoIds.uuid("farm-008"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-011"), DemoIds.uuid("farm-001"), "WARNING", "OPERATION", "Pulverização atrasada", "Backlog no Talhão 01", "OPERATION", DemoIds.uuid("op-002"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-012"), DemoIds.uuid("farm-003"), "INFO", "FIELD", "Scouting concluído", "Talhão A sem pragas críticas", "FIELD", DemoIds.uuid("field-006"), "ACKED", now),
        )
        val existing = repo.findAllById(rows.map { it.id }).map { it.id }.toHashSet()
        repo.saveAll(rows.filter { it.id !in existing })
    }
    private fun AlertEntity.toDto() = AlertDto(id, farmId, severity, type, title, message, entityType, entityId, status, createdAt)
}

@Service
class AlertSeed(private val svc: AlertService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedAlerts() = ApplicationRunner { if (seed) svc.seed() }
}
