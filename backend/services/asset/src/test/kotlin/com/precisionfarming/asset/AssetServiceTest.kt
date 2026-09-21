package com.precisionfarming.asset

import com.precisionfarming.asset.application.AssetService
import com.precisionfarming.asset.application.CreateWorkOrder
import com.precisionfarming.asset.application.UpsertMachine
import com.precisionfarming.asset.infrastructure.MachineEntity
import com.precisionfarming.asset.infrastructure.MachineJpaRepository
import com.precisionfarming.asset.infrastructure.WorkOrderEntity
import com.precisionfarming.asset.infrastructure.WorkOrderJpaRepository
import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.ServiceUnavailableException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
        verify { photos.bind(photo, created.id) }

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
        verify { photos.unbind(photo) }
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
        verify { photos.unbind(photo) }
    }

    @Test
    fun createRejectsPhotoWhenGuardDenies() {
        val photo = UUID.randomUUID()
        every { photos.requireAssignable(photo, farmId, null) } throws
            NotFoundException("FILE_NOT_FOUND", "File not found")
        val ex = assertThrows(NotFoundException::class.java) {
            svc.create(scope, UpsertMachine(farmId, "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE", photo))
        }
        assertEquals("FILE_NOT_FOUND", ex.code)
    }

    @Test
    fun createWorkOrderUsesPostgresMachine() {
        val machineId = UUID.randomUUID()
        every { repo.findById(machineId) } returns Optional.of(
            MachineEntity(machineId, farmId, "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE"),
        )
        every { workOrders.save(any<WorkOrderEntity>()) } answers { firstArg() }
        val created = svc.createWorkOrder(scope, CreateWorkOrder(farmId, machineId, "FILTER_CHANGE", "HIGH"))
        assertEquals(machineId, created.machineId)
        assertEquals(farmId, created.farmId)
    }

    @Test
    fun createWorkOrderRejectsMissingMachine() {
        val machineId = UUID.randomUUID()
        every { repo.findById(machineId) } returns Optional.empty()
        val ex = assertThrows(NotFoundException::class.java) {
            svc.createWorkOrder(scope, CreateWorkOrder(farmId, machineId, "FILTER_CHANGE", "HIGH"))
        }
        assertEquals("MACHINE_NOT_FOUND", ex.code)
    }

    @Test
    fun createWorkOrderRejectsFarmMismatch() {
        val machineId = UUID.randomUUID()
        every { repo.findById(machineId) } returns Optional.of(
            MachineEntity(machineId, DemoIds.uuid("farm-002"), "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE"),
        )
        val ex = assertThrows(ForbiddenException::class.java) {
            svc.createWorkOrder(scope, CreateWorkOrder(farmId, machineId, "FILTER_CHANGE", "HIGH"))
        }
        assertEquals("FARM_SCOPE_DENIED", ex.code)
    }

    @Test
    fun secondMachineSamePhotoIsAlreadyBound() {
        val photo = UUID.randomUUID()
        every { photos.requireAssignable(photo, farmId, null) } throws
            ConflictException("FILE_ALREADY_BOUND", "File is already bound")
        val ex = assertThrows(ConflictException::class.java) {
            svc.create(scope, UpsertMachine(farmId, "Other", "TRACTOR", "John Deere", "8R 410", "IDLE", photo))
        }
        assertEquals("FILE_ALREADY_BOUND", ex.code)
    }

    @Test
    fun bindFailureClearsPhotoFileId() {
        val photo = UUID.randomUUID()
        lateinit var entity: MachineEntity
        every { repo.save(any<MachineEntity>()) } answers { firstArg<MachineEntity>().also { entity = it } }
        every { photos.bind(photo, any()) } throws ServiceUnavailableException()
        assertThrows(ServiceUnavailableException::class.java) {
            svc.create(scope, UpsertMachine(farmId, "JD 8R", "TRACTOR", "John Deere", "8R 410", "IDLE", photo))
        }
        assertNull(entity.photoFileId)
        verify(exactly = 2) { repo.save(any<MachineEntity>()) }
    }
}
