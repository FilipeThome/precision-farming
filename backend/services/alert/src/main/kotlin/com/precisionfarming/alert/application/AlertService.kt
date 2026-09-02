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
            AlertEntity(DemoIds.uuid("alert-001"), DemoIds.uuid("farm-001"), "CRITICAL", "MACHINE", "HIGH_TEMPERATURE", "TRACTOR_TEMP_VARIATION", "MACHINE", DemoIds.uuid("machine-001"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-002"), DemoIds.uuid("farm-001"), "WARNING", "WEATHER", "SPRAY_WINDOW", "RAIN_UNFAVORABLE_SPRAY", "FARM", DemoIds.uuid("farm-001"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-003"), DemoIds.uuid("farm-002"), "INFO", "OPERATION", "OPERATION_PAUSED", "PLANTING_NORTH_PAUSED", "OPERATION", DemoIds.uuid("op-005"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-004"), DemoIds.uuid("farm-003"), "CRITICAL", "MACHINE", "HYDRAULIC_PRESSURE", "PLANTER_PRESSURE_DROP", "MACHINE", DemoIds.uuid("machine-005"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-005"), DemoIds.uuid("farm-004"), "CRITICAL", "OPERATION", "CRITICAL_DELAY", "EAST_PLANTING_WINDOW", "OPERATION", DemoIds.uuid("op-010"), "ACKED", now),
            AlertEntity(DemoIds.uuid("alert-006"), DemoIds.uuid("farm-002"), "WARNING", "MACHINE", "OVERDUE_MAINTENANCE", "TRACTOR_WO_LATE", "MACHINE", DemoIds.uuid("machine-004"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-007"), DemoIds.uuid("farm-005"), "WARNING", "FIELD", "LOW_MOISTURE", "FIELD_WATER_DEFICIT", "FIELD", DemoIds.uuid("field-013"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-008"), DemoIds.uuid("farm-006"), "INFO", "OPERATION", "PLANTING_IN_PROGRESS", "FIELD_VV01_OPERATING", "OPERATION", DemoIds.uuid("op-016"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-009"), DemoIds.uuid("farm-007"), "WARNING", "MACHINE", "SPRAYER_STOPPED", "MACHINE_IN_MAINTENANCE", "MACHINE", DemoIds.uuid("machine-011"), "ACKED", now),
            AlertEntity(DemoIds.uuid("alert-010"), DemoIds.uuid("farm-008"), "INFO", "WEATHER", "FAVORABLE_WINDOW", "NEXT_24H_FERTILIZING", "FARM", DemoIds.uuid("farm-008"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-011"), DemoIds.uuid("farm-001"), "WARNING", "OPERATION", "SPRAY_DELAYED", "FIELD01_BACKLOG", "OPERATION", DemoIds.uuid("op-002"), "OPEN", now),
            AlertEntity(DemoIds.uuid("alert-012"), DemoIds.uuid("farm-003"), "INFO", "FIELD", "SCOUTING_DONE", "FIELD_A_NO_PESTS", "FIELD", DemoIds.uuid("field-006"), "ACKED", now),
        )
        repo.saveAll(rows)
    }
    private fun AlertEntity.toDto() = AlertDto(id, farmId, severity, type, title, message, entityType, entityId, status, createdAt)
}

@Service
class AlertSeed(private val svc: AlertService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedAlerts() = ApplicationRunner { if (seed) svc.seed() }
}
