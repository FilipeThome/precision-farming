package com.precisionfarming.operation

import com.precisionfarming.common.ConflictException
import com.precisionfarming.operation.application.OperationService
import com.precisionfarming.operation.infrastructure.InventorySagaClient
import com.precisionfarming.operation.infrastructure.OperationEntity
import com.precisionfarming.operation.infrastructure.OperationJpaRepository
import com.precisionfarming.operation.infrastructure.SagaEntity
import com.precisionfarming.operation.infrastructure.SagaJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class OperationServiceTest {
    private val repo = mockk<OperationJpaRepository>()
    private val sagas = mockk<SagaJpaRepository>()
    private val inventory = mockk<InventorySagaClient>(relaxUnitFun = true)
    private val svc = OperationService(repo, sagas, inventory)

    private fun scopeFor(op: OperationEntity) = AccessScope(DemoTenant.ID, setOf(op.farmId), "OPERATOR")

    @Test
    fun startFromPlannedReservesInventory() {
        val op = operation("PLANNED")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }

        val dto = svc.start(scopeFor(op), op.id)

        assertEquals("IN_PROGRESS", dto.status)
        verify(exactly = 1) { inventory.move(op.itemId!!, "RESERVE", op.itemQuantity!!, op.id.toString()) }
        verify(exactly = 0) { inventory.move(any(), "RELEASE", any(), any()) }
    }

    @Test
    fun startFromPausedDoesNotReserveAgain() {
        val op = operation("PAUSED")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }

        val dto = svc.start(scopeFor(op), op.id)

        assertEquals("IN_PROGRESS", dto.status)
        verify(exactly = 0) { inventory.move(any(), any(), any(), any()) }
    }

    @Test
    fun startReleasesReservationWhenPersistFails() {
        val op = operation("PLANNED")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } throws RuntimeException("persist failed")

        val ex = assertThrows(ConflictException::class.java) { svc.start(scopeFor(op), op.id) }
        assertEquals("SAGA_FAILED", ex.code)
        verify(exactly = 1) { inventory.move(op.itemId!!, "RESERVE", op.itemQuantity!!, op.id.toString()) }
        verify(exactly = 1) { inventory.move(op.itemId!!, "RELEASE", op.itemQuantity!!, op.id.toString()) }
    }

    @Test
    fun completeRestoresStockWhenPersistFails() {
        val op = operation("IN_PROGRESS")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } throws RuntimeException("persist failed")

        val ex = assertThrows(ConflictException::class.java) { svc.complete(scopeFor(op), op.id) }
        assertEquals("SAGA_FAILED", ex.code)
        verify(exactly = 1) { inventory.move(op.itemId!!, "CONSUME", op.itemQuantity!!, op.id.toString()) }
        verify(exactly = 1) { inventory.move(op.itemId!!, "IN", op.itemQuantity!!, op.id.toString()) }
        verify(exactly = 1) { inventory.move(op.itemId!!, "RESERVE", op.itemQuantity!!, op.id.toString()) }
    }

    private fun operation(status: String) = OperationEntity(
        id = UUID.randomUUID(),
        fieldId = UUID.randomUUID(),
        farmId = UUID.randomUUID(),
        type = "Plantio",
        status = status,
        plannedStart = null,
        plannedEnd = null,
        actualStart = null,
        actualEnd = null,
        machineId = null,
        pauseReason = if (status == "PAUSED") "Chuva" else null,
        itemId = UUID.randomUUID(),
        itemQuantity = BigDecimal("20"),
    )
}
