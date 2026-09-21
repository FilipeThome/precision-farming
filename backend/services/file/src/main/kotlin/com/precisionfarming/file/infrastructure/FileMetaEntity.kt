package com.precisionfarming.file.infrastructure

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "files")
class FileMetaEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID?,
    @Column(name = "field_id") val fieldId: UUID?,
    val kind: String,
    val source: String,
    @Column(name = "object_key") val objectKey: String,
    @Column(name = "acquisition_at") val acquisitionAt: Instant?,
    @Column(name = "processing_version") val processingVersion: String?,
    val quality: String?,
    @Column(name = "content_type") val contentType: String? = null,
    @Column(name = "size_bytes") val sizeBytes: Long? = null,
    @Column(name = "entity_id") val entityId: UUID? = null,
)

@Entity
@Table(name = "map_layers")
class MapLayerEntity(
    @Id val id: UUID,
    @Column(name = "farm_id") val farmId: UUID,
    @Column(name = "field_id") val fieldId: UUID?,
    val name: String,
    val kind: String,
    val source: String,
    @Column(name = "tile_url") val tileUrl: String?,
    @Column(name = "acquired_at") val acquiredAt: Instant?,
    val status: String,
)

interface FileJpaRepository : JpaRepository<FileMetaEntity, UUID> {
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<FileMetaEntity>
}
interface MapLayerJpaRepository : JpaRepository<MapLayerEntity, UUID> {
    fun findByFarmId(farmId: UUID): List<MapLayerEntity>
    fun findByFarmIdIn(farmIds: Collection<UUID>): List<MapLayerEntity>
}
