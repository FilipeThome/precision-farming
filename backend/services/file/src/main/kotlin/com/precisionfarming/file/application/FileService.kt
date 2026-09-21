package com.precisionfarming.file.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.file.domain.FileUploadRules
import com.precisionfarming.file.infrastructure.FileJpaRepository
import com.precisionfarming.file.infrastructure.FileMetaEntity
import com.precisionfarming.file.infrastructure.MapLayerEntity
import com.precisionfarming.file.infrastructure.MapLayerJpaRepository
import com.precisionfarming.security.AccessScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

data class FileMetaDto(
    val id: UUID,
    val farmId: UUID?,
    val fieldId: UUID?,
    val kind: String,
    val source: String,
    val objectKey: String,
    val acquisitionAt: Instant?,
    val processingVersion: String?,
    val quality: String?,
    val contentType: String?,
    val sizeBytes: Long?,
    val entityId: UUID?,
)

data class FileContent(
    val path: Path,
    val contentType: String,
    val sizeBytes: Long,
)

data class MapLayerDto(
    val id: UUID,
    val farmId: UUID,
    val fieldId: UUID?,
    val name: String,
    val kind: String,
    val source: String,
    val tileUrl: String?,
    val acquiredAt: Instant?,
    val status: String,
)

@Service
class FileService(
    private val repo: FileJpaRepository,
    private val layers: MapLayerJpaRepository,
    @Value("\${app.files.storage-dir:./data/files}") storageDir: String,
) {
    private val storageRoot: Path = Path.of(storageDir).toAbsolutePath().normalize()

    fun list(scope: AccessScope) = repo.findByFarmIdIn(scope.farmIds).map { it.toDto() }

    fun get(scope: AccessScope, id: UUID): FileMetaDto {
        val e = repo.findById(id).orElseThrow { NotFoundException("FILE_NOT_FOUND", "File not found") }
        val farmId = e.farmId ?: throw NotFoundException("FILE_NOT_FOUND", "File not found")
        scope.requireFarmRead(farmId, "FILE_NOT_FOUND", "File not found")
        return e.toDto()
    }

    fun content(scope: AccessScope, id: UUID): FileContent {
        val meta = get(scope, id)
        val path = resolve(meta.objectKey)
        if (!Files.isRegularFile(path)) {
            throw NotFoundException("FILE_CONTENT_MISSING", "File content not found")
        }
        return FileContent(
            path,
            meta.contentType ?: "application/octet-stream",
            meta.sizeBytes ?: Files.size(path),
        )
    }

    @Transactional
    fun upload(scope: AccessScope, farmId: UUID, kind: String, entityId: UUID?, bytes: ByteArray): FileMetaDto {
        scope.requireFarm(farmId)
        val validated = FileUploadRules.validate(kind, bytes)
        val id = UUID.randomUUID()
        Files.createDirectories(storageRoot)
        val path = resolve(id.toString())
        Files.write(path, bytes)
        return try {
            repo.save(
                FileMetaEntity(
                    id = id,
                    farmId = farmId,
                    fieldId = null,
                    kind = kind,
                    source = "UPLOAD",
                    objectKey = id.toString(),
                    acquisitionAt = Instant.now(),
                    processingVersion = null,
                    quality = null,
                    contentType = validated.contentType,
                    sizeBytes = bytes.size.toLong(),
                    entityId = entityId,
                ),
            ).toDto()
        } catch (ex: Exception) {
            Files.deleteIfExists(path)
            throw ex
        }
    }

    fun listLayers(scope: AccessScope, farmId: UUID?) =
        layers.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun tileStub(scope: AccessScope, layerId: UUID): Map<String, Any> {
        val layer = layers.findById(layerId).orElseThrow { NotFoundException("LAYER_NOT_FOUND", "Map layer not found") }
        scope.requireEntityFarm(layer.farmId)
        return mapOf(
            "layerId" to layer.id,
            "kind" to layer.kind,
            "format" to "stub",
            "tiles" to listOf(
                mapOf("z" to 12, "x" to 1400, "y" to 2300, "url" to (layer.tileUrl ?: "stub://tile")),
            ),
        )
    }

    @Transactional
    fun seed() {
        repo.save(
            FileMetaEntity(
                DemoIds.uuid("ndvi-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"),
                "NDVI_DEMO", "Demo NDVI", "demo/ndvi/field-001.json", Instant.now(), "0.1.0", "DEMO",
            ),
        )
        val now = Instant.now()
        val layerRows = listOf(
            MapLayerEntity(DemoIds.uuid("layer-ndvi-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "NDVI Talhão 01", "NDVI", "DEMO", "stub://tiles/ndvi/{z}/{x}/{y}", now, "READY"),
            MapLayerEntity(DemoIds.uuid("layer-soil-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"), "Solo P Talhão 01", "SOIL", "DEMO", "stub://tiles/soil/{z}/{x}/{y}", now, "READY"),
            MapLayerEntity(DemoIds.uuid("layer-yield-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-003"), "Produtividade 24/25", "YIELD", "DEMO", "stub://tiles/yield/{z}/{x}/{y}", now, "READY"),
            MapLayerEntity(DemoIds.uuid("layer-ndvi-002"), DemoIds.uuid("farm-002"), DemoIds.uuid("field-004"), "NDVI Norte", "NDVI", "DEMO", "stub://tiles/ndvi/{z}/{x}/{y}", now, "READY"),
            MapLayerEntity(DemoIds.uuid("layer-soil-002"), DemoIds.uuid("farm-003"), DemoIds.uuid("field-006"), "Solo K Talhão A", "SOIL", "DEMO", "stub://tiles/soil/{z}/{x}/{y}", now, "READY"),
            MapLayerEntity(DemoIds.uuid("layer-yield-002"), DemoIds.uuid("farm-004"), DemoIds.uuid("field-009"), "Produtividade Leste", "YIELD", "DEMO", "stub://tiles/yield/{z}/{x}/{y}", now, "READY"),
            MapLayerEntity(DemoIds.uuid("layer-ndvi-003"), DemoIds.uuid("farm-006"), DemoIds.uuid("field-017"), "NDVI VV-01", "NDVI", "DEMO", "stub://tiles/ndvi/{z}/{x}/{y}", now, "READY"),
            MapLayerEntity(DemoIds.uuid("layer-soil-003"), DemoIds.uuid("farm-008"), DemoIds.uuid("field-021"), "Solo P NE-01", "SOIL", "DEMO", "stub://tiles/soil/{z}/{x}/{y}", now, "READY"),
        )
        layers.saveAll(layerRows)
    }

    private fun resolve(objectKey: String): Path {
        val target = storageRoot.resolve(objectKey).normalize()
        require(target.startsWith(storageRoot)) { "Invalid object key" }
        return target
    }

    private fun FileMetaEntity.toDto() = FileMetaDto(
        id, farmId, fieldId, kind, source, objectKey, acquisitionAt, processingVersion, quality,
        contentType, sizeBytes, entityId,
    )

    private fun MapLayerEntity.toDto() = MapLayerDto(id, farmId, fieldId, name, kind, source, tileUrl, acquiredAt, status)
}

@Service
class FileSeed(private val svc: FileService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedFiles() = ApplicationRunner { if (seed) svc.seed() }
}
