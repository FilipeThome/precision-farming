package com.precisionfarming.asset.application

import com.precisionfarming.asset.infrastructure.MachineEntity
import com.precisionfarming.asset.infrastructure.MachineJpaRepository
import com.precisionfarming.asset.infrastructure.WorkOrderEntity
import com.precisionfarming.asset.infrastructure.WorkOrderJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoMachineFarms
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
    fun list(scope: AccessScope, farmId: UUID?) =
        repo.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun get(scope: AccessScope, id: UUID): MachineDto {
        val e = repo.findById(id).orElseThrow { NotFoundException("MACHINE_NOT_FOUND", "Machine not found") }
        scope.requireEntityFarm(e.farmId)
        return e.toDto()
    }

    @Transactional
    fun create(scope: AccessScope, cmd: UpsertMachine): MachineDto {
        scope.requireFarm(cmd.farmId)
        val saved = repo.save(
            MachineEntity(UUID.randomUUID(), cmd.farmId, cmd.name, cmd.type, cmd.manufacturer, cmd.model, cmd.status),
        ).toDto()
        DemoMachineFarms.register(saved.id, saved.farmId)
        return saved
    }

    @Transactional
    fun patch(scope: AccessScope, id: UUID, cmd: UpsertMachine): MachineDto {
        val e = repo.findById(id).orElseThrow { NotFoundException("MACHINE_NOT_FOUND", "Machine not found") }
        scope.requireEntityFarm(e.farmId)
        scope.requireFarm(cmd.farmId)
        e.farmId = cmd.farmId; e.name = cmd.name; e.type = cmd.type
        e.manufacturer = cmd.manufacturer; e.model = cmd.model; e.status = cmd.status
        val saved = repo.save(e).toDto()
        DemoMachineFarms.register(saved.id, saved.farmId)
        return saved
    }

    fun listWorkOrders(scope: AccessScope, farmId: UUID?) =
        workOrders.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createWorkOrder(scope: AccessScope, cmd: CreateWorkOrder): WorkOrderDto {
        scope.requireFarm(cmd.farmId)
        DemoMachineFarms.requireBelongsToFarm(cmd.machineId, cmd.farmId)
        return workOrders.save(
            WorkOrderEntity(UUID.randomUUID(), cmd.farmId, cmd.machineId, cmd.title, cmd.priority, "OPEN", Instant.now(), null),
        ).toDto()
    }

    @Transactional
    fun completeWorkOrder(scope: AccessScope, id: UUID): WorkOrderDto {
        val e = workOrders.findById(id).orElseThrow { NotFoundException("WO_NOT_FOUND", "Work order not found") }
        scope.requireEntityFarm(e.farmId)
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
            Row("machine-006", "farm-002", "Pulverizador 02", "Pulverizador", "Jacto", "Uniport 3030", "IDLE"),
            Row("machine-007", "farm-004", "Trator 03", "Trator", "Case IH", "Magnum 340", "OPERATING"),
            Row("machine-008", "farm-004", "Colheitadeira 02", "Colheitadeira", "John Deere", "S780", "IDLE"),
            Row("machine-009", "farm-005", "Trator 04", "Trator", "Valtra", "BH194", "OPERATING"),
            Row("machine-010", "farm-006", "Plantadeira 02", "Plantadeira", "John Deere", "DB60", "IDLE"),
            Row("machine-011", "farm-007", "Pulverizador 03", "Pulverizador", "Stara", "Imperador 3.0", "MAINTENANCE"),
            Row("machine-012", "farm-008", "Trator 05", "Trator", "New Holland", "T8.380", "OPERATING"),
        )
        val existing = repo.findAllById(rows.map { DemoIds.uuid(it.key) }).map { it.id }.toHashSet()
        repo.saveAll(
            rows.filter { DemoIds.uuid(it.key) !in existing }.map { r ->
                MachineEntity(DemoIds.uuid(r.key), DemoIds.uuid(r.farm), r.name, r.type, r.mfr, r.model, r.status)
            },
        )
        val now = Instant.now()
        val machineFarms = rows.associate { it.key to it.farm }
        val machines = rows.map { it.key }
        val titles = listOf(
            "Troca de filtros", "Revisão hidráulica", "Calibração de GPS", "Troca de óleo",
            "Inspeção de bicos", "Alinhamento", "Substituição de correia", "Diagnóstico motor",
        )
        val woRows = (1..24).map { i ->
            val machine = machines[(i - 1) % machines.size]
            val farm = machineFarms.getValue(machine)
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
        }
        val existingWo = workOrders.findAllById(woRows.map { it.id }).map { it.id }.toHashSet()
        workOrders.saveAll(woRows.filter { it.id !in existingWo })
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
