package com.precisionfarming.operation

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.QueryLimits
import com.precisionfarming.operation.application.CompleteOperation
import com.precisionfarming.operation.application.CreateOperation
import com.precisionfarming.operation.application.OperationService
import com.precisionfarming.operation.infrastructure.AgronomyPrescriptionClient
import com.precisionfarming.operation.infrastructure.InventorySagaClient
import com.precisionfarming.operation.infrastructure.OperationEntity
import com.precisionfarming.operation.infrastructure.OperationJpaRepository
import com.precisionfarming.operation.infrastructure.PrescriptionRef
import com.precisionfarming.operation.infrastructure.SagaEntity
import com.precisionfarming.operation.infrastructure.SagaJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collection
import java.util.Optional
import java.util.UUID

class OperationServiceTest {
    private val repo = mockk<OperationJpaRepository>()
    private val sagas = mockk<SagaJpaRepository>()
    private val inventory = mockk<InventorySagaClient>(relaxUnitFun = true)
    private val agronomy = mockk<AgronomyPrescriptionClient>()
    private val tx = object : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
        override fun commit(status: TransactionStatus) {}
        override fun rollback(status: TransactionStatus) {}
    }
    private val fieldFarms = mockk<com.precisionfarming.security.FieldFarmGuard>(relaxUnitFun = true)
    private val machineFarms = mockk<com.precisionfarming.security.MachineFarmGuard>(relaxUnitFun = true)
    private val itemFarms = mockk<com.precisionfarming.security.ItemFarmGuard>(relaxUnitFun = true)
    private val svc = OperationService(repo, sagas, inventory, agronomy, tx, fieldFarms, machineFarms, itemFarms)

    private fun scopeFor(op: OperationEntity) = AccessScope(DemoTenant.ID, setOf(op.farmId), "OPERATOR")

    @Test
    fun startFromPlannedReservesInventory() {
        val op = operation("PLANNED")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }

        val dto = svc.start(scopeFor(op), op.id)

        assertEquals("IN_PROGRESS", dto.status)
        verify(exactly = 1) { inventory.move(op.itemId!!, "RESERVE", op.itemQuantity!!, op.id.toString(), op.farmId, any()) }
        verify(exactly = 0) { inventory.move(any(), "RELEASE", any(), any(), any(), any()) }
        verify(exactly = 0) { agronomy.get(any(), any()) }
    }

    @Test
    fun startBlockedWhenPrescriptionNotApproved() {
        val rxId = UUID.randomUUID()
        val op = operation("PLANNED").also { it.prescriptionId = rxId }
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }
        every { agronomy.get(rxId, op.farmId) } returns PrescriptionRef(rxId, op.farmId, op.fieldId, "DRAFT")

        val ex = assertThrows(ConflictException::class.java) { svc.start(scopeFor(op), op.id) }
        assertEquals("PRESCRIPTION_NOT_APPROVED", ex.code)
        assertEquals("PLANNED", op.status)
        verify(exactly = 1) { agronomy.get(rxId, op.farmId) }
        verify(exactly = 0) { inventory.move(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { repo.save(any()) }
    }

    @Test
    fun startRethrowsAgronomyNotFoundWithoutChangingStatus() {
        val rxId = UUID.randomUUID()
        val op = operation("PLANNED").also { it.prescriptionId = rxId }
        every { repo.findById(op.id) } returns Optional.of(op)
        every { agronomy.get(rxId, op.farmId) } throws NotFoundException("PRESCRIPTION_NOT_FOUND", "Prescription not found")

        val ex = assertThrows(NotFoundException::class.java) { svc.start(scopeFor(op), op.id) }
        assertEquals("PRESCRIPTION_NOT_FOUND", ex.code)
        assertEquals("PLANNED", op.status)
        verify(exactly = 0) { repo.save(any()) }
    }

    @Test
    fun startWhenApprovedCallsAgronomyThenInventory() {
        val rxId = UUID.randomUUID()
        val op = operation("PLANNED").also { it.prescriptionId = rxId }
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }
        every { agronomy.get(rxId, op.farmId) } returns PrescriptionRef(rxId, op.farmId, op.fieldId, "APPROVED")

        val dto = svc.start(scopeFor(op), op.id)

        assertEquals("IN_PROGRESS", dto.status)
        verifyOrder {
            agronomy.get(rxId, op.farmId)
            inventory.move(op.itemId!!, "RESERVE", op.itemQuantity!!, op.id.toString(), op.farmId, any())
        }
    }

    @Test
    fun completePersistsActualLitersAndPassesToConsume() {
        val op = operation("IN_PROGRESS")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }
        val actual = BigDecimal("12.5")

        val dto = svc.complete(scopeFor(op), op.id, CompleteOperation(actualLiters = actual))

        assertEquals("COMPLETED", dto.status)
        assertEquals(0, actual.compareTo(dto.actualLiters))
        verify(exactly = 1) { inventory.move(op.itemId!!, "CONSUME", actual, op.id.toString(), op.farmId, any()) }
        verify(exactly = 1) {
            inventory.move(op.itemId!!, "RELEASE", BigDecimal("7.5"), op.id.toString(), op.farmId, any())
        }
    }

    @Test
    fun completeFailureDoesNotKeepActualLiters() {
        val op = operation("IN_PROGRESS")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }
        every { inventory.move(any(), "CONSUME", any(), any(), any(), any()) } throws RuntimeException("stock down")

        val ex = assertThrows(ConflictException::class.java) {
            svc.complete(scopeFor(op), op.id, CompleteOperation(actualLiters = BigDecimal("12.5")))
        }
        assertEquals("SAGA_FAILED", ex.code)
        assertEquals("IN_PROGRESS", op.status)
        assertEquals(null, op.actualLiters)
        verify(exactly = 1) { inventory.move(any(), "IN", any(), any(), any(), any()) }
    }

    @Test
    fun completeRetriesUnusedReleaseBeforeRestoringTheFullReservation() {
        val op = operation("IN_PROGRESS")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }
        var releases = 0
        every { inventory.move(any(), "RELEASE", any(), any(), any(), any()) } answers {
            releases += 1
            if (releases == 1) throw RuntimeException("lost release")
        }

        val ex = assertThrows(ConflictException::class.java) {
            svc.complete(scopeFor(op), op.id, CompleteOperation(actualLiters = BigDecimal("12.5")))
        }

        assertEquals("SAGA_FAILED", ex.code)
        assertEquals("IN_PROGRESS", op.status)
        verify(exactly = 1) { inventory.move(any(), "RESERVE", op.itemQuantity!!, op.id.toString(), op.farmId, any()) }
    }

    @Test
    fun completeRejectsNonPositiveOrHugeLitersBeforeStatusChange() {
        val op = operation("IN_PROGRESS")
        every { repo.findById(op.id) } returns Optional.of(op)

        val negative = assertThrows(DomainException::class.java) {
            svc.complete(scopeFor(op), op.id, CompleteOperation(actualLiters = BigDecimal("-1")))
        }
        val huge = assertThrows(DomainException::class.java) {
            svc.complete(scopeFor(op), op.id, CompleteOperation(actualLiters = BigDecimal("100001")))
        }
        assertEquals("ACTUAL_LITERS_INVALID", negative.code)
        assertEquals("ACTUAL_LITERS_INVALID", huge.code)
        assertEquals("IN_PROGRESS", op.status)
        verify(exactly = 0) { repo.save(any()) }
        verify(exactly = 0) { inventory.move(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun completeRejectsLitersAboveReservedQuantity() {
        val op = operation("IN_PROGRESS")
        every { repo.findById(op.id) } returns Optional.of(op)

        val ex = assertThrows(DomainException::class.java) {
            svc.complete(scopeFor(op), op.id, CompleteOperation(actualLiters = BigDecimal("20.1")))
        }
        assertEquals("ACTUAL_LITERS_INVALID", ex.code)
        assertEquals("IN_PROGRESS", op.status)
        verify(exactly = 0) { inventory.move(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun startFromPausedDoesNotReserveAgain() {
        val op = operation("PAUSED")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers { firstArg<OperationEntity>() }

        val dto = svc.start(scopeFor(op), op.id)

        assertEquals("IN_PROGRESS", dto.status)
        verify(exactly = 0) { inventory.move(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun startStaysStartingWhenReleaseFails() {
        val op = operation("PLANNED")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        val saved = mutableListOf<String>()
        every { repo.save(any()) } answers {
            val entity = firstArg<OperationEntity>()
            saved += entity.status
            if (entity.status == "IN_PROGRESS") throw RuntimeException("persist failed")
            entity
        }
        every { inventory.move(any(), "RELEASE", any(), any(), any(), any()) } throws RuntimeException("release down")

        val ex = assertThrows(ConflictException::class.java) { svc.start(scopeFor(op), op.id) }

        assertEquals("SAGA_COMPENSATION_FAILED", ex.code)
        assertTrue(saved.contains("STARTING"))
        assertTrue(saved.none { it == "PLANNED" })
    }

    @Test
    fun startReleasesReservationWhenPersistFails() {
        val op = operation("PLANNED")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers {
            val e = firstArg<OperationEntity>()
            if (e.status == "IN_PROGRESS") throw RuntimeException("persist failed")
            e
        }

        val ex = assertThrows(ConflictException::class.java) { svc.start(scopeFor(op), op.id) }
        assertEquals("SAGA_FAILED", ex.code)
        verify(exactly = 1) { inventory.move(op.itemId!!, "RESERVE", op.itemQuantity!!, op.id.toString(), op.farmId, any()) }
        verify(exactly = 1) { inventory.move(op.itemId!!, "RELEASE", op.itemQuantity!!, op.id.toString(), op.farmId, any()) }
    }

    @Test
    fun completeRestoresStockWhenPersistFails() {
        val op = operation("IN_PROGRESS")
        every { repo.findById(op.id) } returns Optional.of(op)
        every { sagas.save(any()) } answers { firstArg<SagaEntity>() }
        every { repo.save(any()) } answers {
            val e = firstArg<OperationEntity>()
            if (e.status == "COMPLETED") throw RuntimeException("persist failed")
            e
        }

        val ex = assertThrows(ConflictException::class.java) { svc.complete(scopeFor(op), op.id) }
        assertEquals("SAGA_FAILED", ex.code)
        verify(exactly = 1) { inventory.move(op.itemId!!, "CONSUME", op.itemQuantity!!, op.id.toString(), op.farmId, any()) }
        verify(exactly = 1) { inventory.move(op.itemId!!, "IN", op.itemQuantity!!, op.id.toString(), op.farmId, any()) }
        verify(exactly = 1) { inventory.move(op.itemId!!, "RESERVE", op.itemQuantity!!, op.id.toString(), op.farmId, any()) }
    }

    @Test
    fun machineSummaryKeepsOpsInsideWindow() {
        val machineId = DemoIds.uuid("machine-001")
        val farmId = DemoIds.uuid("farm-001")
        val inWindow = operation("COMPLETED").also {
            it.farmId = farmId
            it.machineId = machineId
            it.actualStart = Instant.parse("2026-09-06T10:00:00Z")
            it.areaHa = BigDecimal("10.0")
        }
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val to = Instant.parse("2026-09-08T00:00:00Z")
        every { repo.findWorkInWindow(machineId, setOf(farmId), from, to) } returns listOf(inWindow)
        val dto = svc.machineSummary(
            AccessScope(DemoTenant.ID, setOf(farmId), "OPERATOR"),
            machineId,
            Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2026-09-08T00:00:00Z"),
        )
        assertEquals(BigDecimal("10.0"), dto.areaHa)
        verify { repo.findWorkInWindow(machineId, setOf(farmId), from, to) }
    }

    @Test
    fun workWindowQueryExcludesRowsOutsideTheRequestedInstants() {
        val query = OperationJpaRepository::class.java.getMethod(
            "findWorkInWindow",
            UUID::class.java,
            Collection::class.java,
            Instant::class.java,
            Instant::class.java,
        ).getAnnotation(Query::class.java)
        val jpql = query.value.replace(Regex("\\s+"), " ")
        assertTrue(jpql.contains("o.actualStart >= :from"))
        assertTrue(jpql.contains("o.actualStart <= :to"))
        assertTrue(jpql.contains("o.actualStart is null"))
        assertTrue(jpql.contains("o.plannedStart >= :from"))
        assertTrue(jpql.contains("o.plannedStart <= :to"))
    }

    @Test
    fun machineSummaryUnknownMachineIsNotFound() {
        val machineId = UUID.randomUUID()
        every { machineFarms.requireRead(any(), machineId) } throws NotFoundException("MACHINE_NOT_FOUND", "Not found")
        val ex = assertThrows(NotFoundException::class.java) {
            svc.machineSummary(
                AccessScope(DemoTenant.ID, setOf(DemoIds.uuid("farm-001")), "OPERATOR"),
                machineId,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-08T00:00:00Z"),
            )
        }
        assertEquals("MACHINE_NOT_FOUND", ex.code)
    }

    @Test
    fun machineSummaryOutOfScopeIsNotFound() {
        val machineId = DemoIds.uuid("machine-001")
        every { machineFarms.requireRead(any(), machineId) } throws NotFoundException("MACHINE_NOT_FOUND", "Not found")
        val ex = assertThrows(NotFoundException::class.java) {
            svc.machineSummary(
                AccessScope(DemoTenant.ID, setOf(DemoIds.uuid("farm-002")), "OPERATOR"),
                machineId,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-08T00:00:00Z"),
            )
        }
        assertEquals("MACHINE_NOT_FOUND", ex.code)
    }

    @Test
    fun machineSummaryRejectsInvertedRange() {
        val ex = assertThrows(DomainException::class.java) {
            svc.machineSummary(
                AccessScope(DemoTenant.ID, setOf(DemoIds.uuid("farm-001")), "OPERATOR"),
                DemoIds.uuid("machine-001"),
                Instant.parse("2026-09-08T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
            )
        }
        assertEquals("OPERATION_RANGE_EXCEEDED", ex.code)
    }

    @Test
    fun machineSummaryCountsEveryOpInRange() {
        val machineId = DemoIds.uuid("machine-001")
        val farmId = DemoIds.uuid("farm-001")
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val to = Instant.parse("2026-09-08T00:00:00Z")
        val ops = (0 until QueryLimits.MAX_LIST + 1).map { i ->
            operation("COMPLETED").also {
                it.farmId = farmId
                it.machineId = machineId
                it.actualStart = Instant.parse("2026-09-02T10:00:00Z").plusSeconds(i.toLong())
                it.areaHa = BigDecimal("1.0")
            }
        }
        every { repo.findWorkInWindow(machineId, setOf(farmId), from, to) } returns ops
        val dto = svc.machineSummary(
            AccessScope(DemoTenant.ID, setOf(farmId), "OPERATOR"),
            machineId,
            from,
            to,
        )
        assertEquals(BigDecimal("${QueryLimits.MAX_LIST + 1}.0"), dto.areaHa)
    }

    @Test
    fun createRejectsNegativeArea() {
        val farmId = DemoIds.uuid("farm-001")
        val ex = assertThrows(DomainException::class.java) {
            svc.create(
                AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER"),
                CreateOperation(
                    DemoIds.uuid("field-001"),
                    farmId,
                    "PLANTING",
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal("-1"),
                ),
            )
        }
        assertEquals("OPERATION_AREA_INVALID", ex.code)
        verify(exactly = 0) { repo.save(any()) }
    }

    @Test
    fun createAllowsCadastroIdsWhenGuardsPass() {
        val farmId = UUID.randomUUID()
        val fieldId = UUID.randomUUID()
        val machineId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        every { repo.save(any<OperationEntity>()) } answers { firstArg() }
        val created = svc.create(
            AccessScope(DemoTenant.ID, setOf(farmId), "FARM_MANAGER"),
            CreateOperation(fieldId, farmId, "PLANTING", null, null, machineId, itemId, BigDecimal.ONE, null),
        )
        assertEquals(machineId, created.machineId)
        assertEquals(itemId, created.itemId)
        verify { fieldFarms.requireBelongsToFarm(fieldId, farmId) }
        verify { machineFarms.requireBelongsToFarm(machineId, farmId) }
        verify { itemFarms.requireBelongsToFarm(itemId, farmId) }
    }

    @Test
    fun seedRewritesExistingOperationTimestamps() {
        val id = DemoIds.uuid("op-001")
        val stale = Instant.parse("2020-01-01T10:00:00Z")
        val existing = OperationEntity(
            id,
            DemoIds.uuid("field-001"),
            DemoIds.uuid("farm-001"),
            "PLANTING",
            "COMPLETED",
            stale,
            stale.plus(1, ChronoUnit.DAYS),
            stale,
            stale.plus(6, ChronoUnit.HOURS),
            DemoIds.uuid("machine-001"),
            null,
            DemoIds.uuid("item-001"),
            BigDecimal("20"),
            BigDecimal("120.5"),
        )
        every { repo.findAllById(any<Iterable<UUID>>()) } returns listOf(existing)
        lateinit var saved: List<OperationEntity>
        every { repo.saveAll(any<Iterable<OperationEntity>>()) } answers {
            firstArg<Iterable<OperationEntity>>().toList().also { saved = it }
        }

        svc.seed()

        val updated = saved.first { it.id == id }
        val weekAgo = Instant.now().minus(8, ChronoUnit.DAYS)
        assertTrue(updated.plannedStart!!.isAfter(weekAgo))
        assertTrue(updated.actualStart!!.isAfter(weekAgo))
        assertTrue(updated.actualEnd!!.isAfter(weekAgo))
    }

    @Test
    fun getHidesOutOfScopeAsNotFound() {
        val op = operation("PLANNED")
        every { repo.findById(op.id) } returns Optional.of(op)
        val other = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "OPERATOR")
        val ex = assertThrows(NotFoundException::class.java) { svc.get(other, op.id) }
        assertEquals("OPERATION_NOT_FOUND", ex.code)
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
