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

interface MachineJpaRepository : JpaRepository<MachineEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<MachineEntity>
}
