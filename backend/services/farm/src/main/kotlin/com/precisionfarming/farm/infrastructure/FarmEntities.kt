package com.precisionfarming.farm.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "farms")
class FarmEntity(
    @Id val id: UUID,
    var name: String,
    var location: String,
    @Column(name = "area_ha") var areaHa: BigDecimal,
    var timezone: String,
)

@Entity
@Table(name = "fields")
class FieldEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") var farmId: UUID,
    var name: String,
    @Column(name = "area_ha") var areaHa: BigDecimal,
    var crop: String,
    var variety: String?,
    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    var geometry: org.locationtech.jts.geom.Geometry,
    @Column(columnDefinition = "geometry(Point,4326)")
    var centroid: org.locationtech.jts.geom.Geometry?,
)

@Entity
@Table(name = "seasons")
class SeasonEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") var farmId: UUID,
    var name: String,
    var crop: String,
    @Column(name = "start_date") var startDate: LocalDate,
    @Column(name = "end_date") var endDate: LocalDate?,
    var status: String,
)

interface FarmJpaRepository : JpaRepository<FarmEntity, UUID>
interface FieldJpaRepository : JpaRepository<FieldEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<FieldEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<FieldEntity>
    fun deleteByFarmId(farmId: UUID): Long
}
interface SeasonJpaRepository : JpaRepository<SeasonEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<SeasonEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<SeasonEntity>
}
