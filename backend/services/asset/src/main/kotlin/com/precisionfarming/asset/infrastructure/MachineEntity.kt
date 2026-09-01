package com.precisionfarming.asset.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "machines")
class MachineEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") var farmId: UUID,
    var name: String,
    var type: String,
    var manufacturer: String,
    var model: String,
    var status: String,
)

@Entity
@Table(name = "maintenance_work_orders")
class WorkOrderEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "machine_id") val machineId: UUID,
    var title: String,
    var priority: String,
    var status: String,
    @Column(name = "created_at") val createdAt: java.time.Instant,
    @Column(name = "completed_at") var completedAt: java.time.Instant? = null,
)

interface MachineJpaRepository : JpaRepository<MachineEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<MachineEntity>
}

interface WorkOrderJpaRepository : JpaRepository<WorkOrderEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<WorkOrderEntity>
}
