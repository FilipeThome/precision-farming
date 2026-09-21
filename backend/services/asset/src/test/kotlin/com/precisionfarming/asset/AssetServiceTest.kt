package com.precisionfarming.asset

import com.precisionfarming.asset.application.AssetService
import com.precisionfarming.asset.application.UpsertMachine
import com.precisionfarming.asset.infrastructure.MachineEntity
import com.precisionfarming.asset.infrastructure.MachineJpaRepository
import com.precisionfarming.asset.infrastructure.WorkOrderJpaRepository
import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class AssetServiceTest {
    private val repo = mockk<MachineJpaRepository>()
    private val workOrders = mockk<WorkOrderJpaRepository>()
    private val photos = mockk<com.precisionfarming.asset.infrastructure.MachinePhotoGuard>(relaxUnitFun = true)
    private val svc = AssetService(repo, workOrders, photos)
    private val farmId = DemoIds.uuid("farm-001")
    private val scope = AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER")

    @Test
    fun createAndPatchRoundTripPhotoFileId() {
        every { repo.save(any<MachineEntity>()) } answers { firstArg() }
        val photo = UUID.randomUUID()
        val created = svc.create(
            scope,
            UpsertMachine(farmId, "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE", photo),
        )
        assertEquals(photo, created.photoFileId)
        assertEquals("/api/v1/files/$photo/content", created.photoUrl)

        every { repo.findById(created.id) } returns Optional.of(
            MachineEntity(created.id, farmId, created.name, created.type, created.manufacturer, created.model, created.status, photo),
        )
        val patched = svc.patch(
            scope,
            created.id,
            UpsertMachine(farmId, "JD 8R", "TRACTOR", "John Deere", "8R 410", "OPERATING", null),
        )
        assertNull(patched.photoFileId)
        assertNull(patched.photoUrl)
    }

    @Test
    fun movingFarmClearsPreviousPhoto() {
        every { repo.save(any<MachineEntity>()) } answers { firstArg() }
        val photo = UUID.randomUUID()
        val created = svc.create(
            scope,
            UpsertMachine(farmId, "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE", photo),
        )
        val other = DemoIds.uuid("farm-002")
        val otherScope = AccessScope(DemoTenant.ID, setOf(farmId, other), "FARM_MANAGER")
        every { repo.findById(created.id) } returns Optional.of(
            MachineEntity(created.id, farmId, created.name, created.type, created.manufacturer, created.model, created.status, photo),
        )
        val moved = svc.patch(
            otherScope,
            created.id,
            UpsertMachine(other, "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE", photo),
        )
        assertNull(moved.photoFileId)
    }

    @Test
    fun createRejectsPhotoWhenGuardDenies() {
        val photo = UUID.randomUUID()
        every { photos.requireMachinePhoto(photo, farmId) } throws
            com.precisionfarming.common.NotFoundException("FILE_NOT_FOUND", "File not found")
        val ex = org.junit.jupiter.api.Assertions.assertThrows(
            com.precisionfarming.common.NotFoundException::class.java,
        ) {
            svc.create(scope, UpsertMachine(farmId, "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE", photo))
        }
        assertEquals("FILE_NOT_FOUND", ex.code)
    }
}
