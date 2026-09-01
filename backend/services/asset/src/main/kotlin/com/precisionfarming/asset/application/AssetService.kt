package com.precisionfarming.asset.application

import com.precisionfarming.asset.infrastructure.MachineEntity
import com.precisionfarming.asset.infrastructure.MachineJpaRepository
import com.precisionfarming.asset.infrastructure.WorkOrderEntity
import com.precisionfarming.asset.infrastructure.WorkOrderJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class MachineDto(
    val id: UUID, val farmId: UUID, val name: String, val type: String,
    val manufacturer: String, val model: String, val status: String,
)
data class UpsertMachine(
    val farmId: UUID, val name: String, val type: String,
    val manufacturer: String, val model: String, val status: String,
)
data class WorkOrderDto(
    val id: UUID, val farmId: UUID, val machineId: UUID, val title: String,
    val priority: String, val status: String, val createdAt: Instant, val completedAt: Instant?,
)
data class CreateWorkOrder(val farmId: UUID, val machineId: UUID, val title: String, val priority: String)

@Service
class AssetService(
    private val repo: MachineJpaRepository,
    private val workOrders: WorkOrderJpaRepository,
) {
    fun list(farmId: UUID?) = (farmId?.let { repo.findByFarmId(it) } ?: repo.findAll()).map { it.toDto() }
    fun get(id: UUID) = repo.findById(id).orElseThrow { NotFoundException("MACHINE_NOT_FOUND", "Machine not found") }.toDto()

    @Transactional
    fun create(cmd: UpsertMachine) =
        repo.save(MachineEntity(UUID.randomUUID(), cmd.farmId, cmd.name, cmd.type, cmd.manufacturer, cmd.model, cmd.status)).toDto()

    @Transactional
    fun patch(id: UUID, cmd: UpsertMachine): MachineDto {
        val e = repo.findById(id).orElseThrow { NotFoundException("MACHINE_NOT_FOUND", "Machine not found") }
        e.farmId = cmd.farmId; e.name = cmd.name; e.type = cmd.type
        e.manufacturer = cmd.manufacturer; e.model = cmd.model; e.status = cmd.status
        return repo.save(e).toDto()
    }

    fun listWorkOrders(farmId: UUID?) =
        (farmId?.let { workOrders.findByFarmId(it) } ?: workOrders.findAll()).map { it.toDto() }

    @Transactional
    fun createWorkOrder(cmd: CreateWorkOrder) =
        workOrders.save(
            WorkOrderEntity(UUID.randomUUID(), cmd.farmId, cmd.machineId, cmd.title, cmd.priority, "OPEN", Instant.now(), null),
        ).toDto()

    @Transactional
    fun completeWorkOrder(id: UUID): WorkOrderDto {
        val e = workOrders.findById(id).orElseThrow { NotFoundException("WO_NOT_FOUND", "Work order not found") }
        e.status = "COMPLETED"
        e.completedAt = Instant.now()
        return workOrders.save(e).toDto()
    }

    @Transactional
    fun seed() {
        data class Row(val key: String, val farm: String, val name: String, val type: String, val mfr: String, val model: String, val status: String)
        val rows = listOf(
            Row("machine-001", "farm-001", "Trator 01", "Trator", "John Deere", "8R 410", "OPERATING"),
            Row("machine-002", "farm-001", "Pulverizador 01", "Pulverizador", "John Deere", "R4045", "OPERATING"),
            Row("machine-003", "farm-001", "Colheitadeira 01", "Colheitadeira", "New Holland", "CR7.90", "IDLE"),
            Row("machine-004", "farm-002", "Trator 02", "Trator", "Massey Ferguson", "8737", "MAINTENANCE"),
            Row("machine-005", "farm-003", "Plantadeira 01", "Plantadeira", "Stara", "Absoluta", "OPERATING"),
        )
        val existing = repo.findAllById(rows.map { DemoIds.uuid(it.key) }).map { it.id }.toHashSet()
        repo.saveAll(
            rows.filter { DemoIds.uuid(it.key) !in existing }.map { r ->
                MachineEntity(DemoIds.uuid(r.key), DemoIds.uuid(r.farm), r.name, r.type, r.mfr, r.model, r.status)
            },
        )
        if (!workOrders.existsById(DemoIds.uuid("wo-001"))) {
            val now = Instant.now()
            val machines = listOf("machine-001", "machine-002", "machine-003", "machine-004", "machine-005")
            val titles = listOf(
                "Troca de filtros", "Revisão hidráulica", "Calibração de GPS", "Troca de óleo",
                "Inspeção de bicos", "Alinhamento", "Substituição de correia", "Diagnóstico motor",
            )
            workOrders.saveAll(
                (1..20).map { i ->
                    val machine = machines[(i - 1) % machines.size]
                    val farm = when {
                        i <= 12 -> "farm-001"
                        i <= 16 -> "farm-002"
                        else -> "farm-003"
                    }
                    val completed = i % 4 == 0
                    WorkOrderEntity(
                        DemoIds.uuid("wo-%03d".format(i)),
                        DemoIds.uuid(farm),
                        DemoIds.uuid(machine),
                        titles[(i - 1) % titles.size] + " #$i",
                        listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")[i % 4],
                        if (completed) "COMPLETED" else "OPEN",
                        now.minus(i.toLong(), ChronoUnit.DAYS),
                        if (completed) now.minus((i - 1).toLong(), ChronoUnit.DAYS) else null,
                    )
                },
            )
        }
    }

    private fun MachineEntity.toDto() = MachineDto(id, farmId, name, type, manufacturer, model, status)
    private fun WorkOrderEntity.toDto() = WorkOrderDto(id, farmId, machineId, title, priority, status, createdAt, completedAt)
}

@Service
class AssetSeed(
    private val svc: AssetService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedMachines() = ApplicationRunner { if (seed) svc.seed() }
}
