package com.precisionfarming.compliance.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "traceability_records")
class TraceabilityEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID?,
    @Column(name = "lot_code") val lotCode: String,
    val crop: String,
    @Column(name = "event_type") val eventType: String,
    val summary: String,
    @Column(name = "occurred_at") val occurredAt: Instant,
)

@Entity
@Table(name = "esg_metrics")
class EsgMetricEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    val metric: String,
    val value: BigDecimal,
    val unit: String,
    @Column(name = "period_label") val periodLabel: String,
    val score: BigDecimal?,
)

@Entity
@Table(name = "evidence_packs")
class EvidencePackEntity(
    @Id @Column(name = "lot_code") val lotCode: String,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "farm_name") val farmName: String,
    @Column(name = "field_id") val fieldId: UUID,
    @Column(name = "field_name") val fieldName: String,
    @Column(name = "polygon_geojson") val polygonGeoJson: String,
    @Column(name = "input_refs") val inputRefs: String,
    @Column(name = "receituario_number") val receituarioNumber: String?,
    @Column(name = "active_ingredient") val activeIngredient: String?,
    @Column(name = "moa_group") val moaGroup: String?,
    @Column(name = "responsible_tech_cpf") val responsibleTechCpf: String?,
    @Column(name = "phi_days") val phiDays: Int?,
    @Column(name = "deforestation_cutoff_date") val deforestationCutoffDate: LocalDate,
    val embargoed: Boolean,
    @Column(name = "car_status") val carStatus: String,
)

@Entity
@Table(name = "credit_dossiers")
class CreditDossierEntity(
    @Id @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "car_code") val carCode: String,
    @Column(name = "car_status") val carStatus: String,
    val embargoed: Boolean,
    @Column(name = "deforestation_cutoff_date") val deforestationCutoffDate: LocalDate,
    @Column(name = "deforestation_clear") val deforestationClear: Boolean,
    @Column(name = "zarc_compliant") val zarcCompliant: Boolean,
    @Column(name = "remote_sensing_note") val remoteSensingNote: String?,
)

interface TraceabilityJpaRepository : JpaRepository<TraceabilityEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<TraceabilityEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<TraceabilityEntity>
    fun findByIdAndFarmId(id: UUID, farmId: UUID): TraceabilityEntity?
}

interface EsgMetricJpaRepository : JpaRepository<EsgMetricEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<EsgMetricEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<EsgMetricEntity>
}

interface EvidencePackJpaRepository : JpaRepository<EvidencePackEntity, String>

interface CreditDossierJpaRepository : JpaRepository<CreditDossierEntity, UUID>
