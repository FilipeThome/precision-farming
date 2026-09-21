package com.precisionfarming.file

import com.precisionfarming.common.DomainException
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.file.application.BindFile
import com.precisionfarming.file.application.FileService
import com.precisionfarming.file.infrastructure.FileJpaRepository
import com.precisionfarming.file.infrastructure.FileMetaEntity
import com.precisionfarming.file.infrastructure.MapLayerJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.UUID

class FileServiceTest {
    @TempDir
    lateinit var dir: Path

    private val repo = mockk<FileJpaRepository>()
    private val layers = mockk<MapLayerJpaRepository>()
    private val farmId = UUID.randomUUID()
    private val scope = AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER")

    @Test
    fun uploadPersistsBytesAndMetadata() {
        every { repo.save(any<FileMetaEntity>()) } answers { firstArg() }
        val svc = FileService(repo, layers, dir.toString())
        val jpeg = jpegBytes()

        val dto = svc.upload(scope, farmId, "MACHINE_PHOTO", null, jpeg)

        assertEquals("MACHINE_PHOTO", dto.kind)
        assertEquals("UPLOAD", dto.source)
        assertEquals("image/jpeg", dto.contentType)
        assertEquals(jpeg.size.toLong(), dto.sizeBytes)
        assertTrue(Files.isRegularFile(dir.resolve(dto.id.toString())))
        assertEquals(null, dto.entityId)
    }

    @Test
    fun uploadIgnoresEntityIdQueryParam() {
        every { repo.save(any<FileMetaEntity>()) } answers { firstArg() }
        val svc = FileService(repo, layers, dir.toString())
        val dto = svc.upload(scope, farmId, "MACHINE_PHOTO", UUID.randomUUID(), jpegBytes())
        assertEquals(null, dto.entityId)
    }

    @Test
    fun uploadRejectsOtherFarm() {
        val svc = FileService(repo, layers, dir.toString())
        val ex = assertThrows(ForbiddenException::class.java) {
            svc.upload(scope, UUID.randomUUID(), "MACHINE_PHOTO", null, jpegBytes())
        }
        assertEquals("FARM_SCOPE_DENIED", ex.code)
    }

    @Test
    fun uploadRejectsUnsupportedKind() {
        val svc = FileService(repo, layers, dir.toString())
        val ex = assertThrows(DomainException::class.java) {
            svc.upload(scope, farmId, "NDVI_DEMO", null, jpegBytes())
        }
        assertEquals("FILE_KIND_UNSUPPORTED", ex.code)
    }

    @Test
    fun contentLooksMissingForOtherFarm() {
        val id = UUID.randomUUID()
        every { repo.findById(id) } returns Optional.of(meta(id, farmId, id.toString(), "image/jpeg"))
        val svc = FileService(repo, layers, dir.toString())
        val other = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "FARM_MANAGER")
        val ex = assertThrows(NotFoundException::class.java) {
            svc.content(other, id)
        }
        assertEquals("FILE_NOT_FOUND", ex.code)
    }

    @Test
    fun contentRejectsMissingBlob() {
        val id = UUID.randomUUID()
        every { repo.findById(id) } returns Optional.of(meta(id, farmId, "missing-key", "image/jpeg"))
        val svc = FileService(repo, layers, dir.toString())
        val ex = assertThrows(NotFoundException::class.java) {
            svc.content(scope, id)
        }
        assertEquals("FILE_CONTENT_MISSING", ex.code)
    }

    @Test
    fun bindIsExclusiveAndUnbindClearsEntity() {
        val id = UUID.randomUUID()
        val machineId = UUID.randomUUID()
        val entity = meta(id, farmId, id.toString(), "image/jpeg")
        every { repo.findById(id) } returns Optional.of(entity)
        every { repo.findByKindAndEntityId("MACHINE_PHOTO", machineId) } returns null
        every { repo.save(any<FileMetaEntity>()) } answers { firstArg() }
        val svc = FileService(repo, layers, dir.toString())

        val bound = svc.bind(scope, id, BindFile(machineId))
        assertEquals(machineId, bound.entityId)

        val otherFile = meta(UUID.randomUUID(), farmId, "other", "image/jpeg").also { it.entityId = machineId }
        every { repo.findByKindAndEntityId("MACHINE_PHOTO", machineId) } returns otherFile
        val ex = assertThrows(com.precisionfarming.common.ConflictException::class.java) {
            svc.bind(scope, id, BindFile(machineId))
        }
        assertEquals("FILE_ALREADY_BOUND", ex.code)

        entity.entityId = machineId
        every { repo.findByKindAndEntityId("MACHINE_PHOTO", any()) } returns entity
        val unbound = svc.bind(scope, id, BindFile(null))
        assertEquals(null, unbound.entityId)
    }

    @Test
    fun bindRejectsFileAlreadyBoundToAnotherEntity() {
        val id = UUID.randomUUID()
        val entity = meta(id, farmId, id.toString(), "image/jpeg")
        entity.entityId = UUID.randomUUID()
        every { repo.findById(id) } returns Optional.of(entity)
        val svc = FileService(repo, layers, dir.toString())
        val ex = assertThrows(com.precisionfarming.common.ConflictException::class.java) {
            svc.bind(scope, id, BindFile(UUID.randomUUID()))
        }
        assertEquals("FILE_ALREADY_BOUND", ex.code)
    }

    @Test
    fun bindRejectsOtherFarm() {
        val id = UUID.randomUUID()
        every { repo.findById(id) } returns Optional.of(meta(id, farmId, id.toString(), "image/jpeg"))
        val svc = FileService(repo, layers, dir.toString())
        val other = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "FARM_MANAGER")
        val ex = assertThrows(ForbiddenException::class.java) {
            svc.bind(other, id, BindFile(UUID.randomUUID()))
        }
        assertEquals("FARM_SCOPE_DENIED", ex.code)
    }

    private fun jpegBytes() = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(13)

    private fun meta(id: UUID, farm: UUID, key: String, contentType: String) = FileMetaEntity(
        id = id,
        farmId = farm,
        fieldId = null,
        kind = "MACHINE_PHOTO",
        source = "UPLOAD",
        objectKey = key,
        acquisitionAt = Instant.parse("2026-09-21T00:00:00Z"),
        processingVersion = null,
        quality = null,
        contentType = contentType,
        sizeBytes = 16,
        entityId = null,
    )
}
