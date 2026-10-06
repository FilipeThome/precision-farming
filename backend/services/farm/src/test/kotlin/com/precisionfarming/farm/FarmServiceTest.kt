package com.precisionfarming.farm

import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.DomainException
import com.precisionfarming.farm.application.FarmService
import com.precisionfarming.farm.application.UpsertFarm
import com.precisionfarming.farm.application.UpsertField
import com.precisionfarming.farm.infrastructure.FarmEntity
import com.precisionfarming.farm.infrastructure.AuthMembershipClient
import com.precisionfarming.farm.infrastructure.FarmJpaRepository
import com.precisionfarming.farm.infrastructure.FieldEntity
import com.precisionfarming.farm.infrastructure.FieldJpaRepository
import com.precisionfarming.farm.infrastructure.SeasonJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class FarmServiceTest {
    private val farms = mockk<FarmJpaRepository>()
    private val fields = mockk<FieldJpaRepository>()
    private val seasons = mockk<SeasonJpaRepository>()
    private val memberships = mockk<AuthMembershipClient>(relaxed = true)
    private val tx = object : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
        override fun commit(status: TransactionStatus) {}
        override fun rollback(status: TransactionStatus) {}
    }
    private val svc = FarmService(farms, fields, seasons, memberships, tx)

    private fun scope(farmId: UUID, userId: UUID? = null) =
        AccessScope(DemoTenant.ID, setOf(farmId), "ADMIN", userId)

    @Test
    fun createFarmGrantsMembershipAndDropsTheRowWhenAuthFails() {
        val userId = UUID.randomUUID()
        every { farms.save(any()) } answers { firstArg<FarmEntity>() }
        every { farms.deleteById(any()) } returns Unit
        every { memberships.grant(userId, any()) } throws IllegalStateException("auth down")

        assertThrows(IllegalStateException::class.java) {
            svc.createFarm(
                scope(UUID.randomUUID(), userId),
                UpsertFarm("Nova", "MS", BigDecimal.TEN, "America/Campo_Grande"),
            )
        }
        verify { farms.deleteById(any()) }
    }

    @Test
    fun createFarmKeepsTheAuthErrorWhenTheCompensatingDeleteFails() {
        val userId = UUID.randomUUID()
        every { farms.save(any()) } answers { firstArg<FarmEntity>() }
        every { farms.deleteById(any()) } throws IllegalStateException("delete down")
        every { memberships.grant(userId, any()) } throws IllegalStateException("auth down")

        val ex = assertThrows(IllegalStateException::class.java) {
            svc.createFarm(
                scope(UUID.randomUUID(), userId),
                UpsertFarm("Nova", "MS", BigDecimal.TEN, "America/Campo_Grande"),
            )
        }
        assertEquals("auth down", ex.message)
        assertEquals("delete down", ex.suppressed.single().message)
    }

    @Test
    fun createFieldPersistsParsedGeoJsonCoordinates() {
        val farmId = UUID.randomUUID()
        every { farms.existsById(farmId) } returns true
        every { fields.save(any()) } answers { firstArg<FieldEntity>() }

        val dto = svc.createField(
            scope(farmId),
            UpsertField(
                farmId = farmId,
                name = "Talhão Norte",
                areaHa = BigDecimal("10"),
                crop = "Soja",
                variety = null,
                geometry = """{"type":"Polygon","coordinates":[[[-50.92,-17.79],[-50.88,-17.79],[-50.88,-17.75],[-50.92,-17.75],[-50.92,-17.79]]]}""",
            ),
        )

        assertTrue(dto.geometry.contains("-50.92"))
        assertTrue(dto.geometry.contains("-17.79"))
        assertFalse(dto.geometry.contains("-54.57"))
        assertFalse(dto.geometry.contains("-19.39"))
    }

    @Test
    fun createFieldRejectsNonPolygonGeoJson() {
        val farmId = UUID.randomUUID()
        every { farms.existsById(farmId) } returns true

        val ex = assertThrows(DomainException::class.java) {
            svc.createField(
                scope(farmId),
                UpsertField(
                    farmId = farmId,
                    name = "Ponto",
                    areaHa = BigDecimal.ONE,
                    crop = "Soja",
                    variety = null,
                    geometry = """{"type":"Point","coordinates":[-50.92,-17.79]}""",
                ),
            )
        }
        assertEquals("INVALID_GEOMETRY", ex.code)
    }

    @Test
    fun patchFieldSameFarmUpdatesAttributes() {
        val farmId = UUID.randomUUID()
        lateinit var saved: FieldEntity
        every { farms.existsById(farmId) } returns true
        every { fields.save(any()) } answers { firstArg<FieldEntity>().also { saved = it } }
        every { fields.findById(any()) } answers { Optional.of(saved) }
        val polygon =
            """{"type":"Polygon","coordinates":[[[-50.92,-17.79],[-50.88,-17.79],[-50.88,-17.75],[-50.92,-17.75],[-50.92,-17.79]]]}"""
        val created = svc.createField(
            scope(farmId),
            UpsertField(farmId, "Talhão Norte", BigDecimal("10"), "Soja", null, polygon),
        )
        val patched = svc.patchField(
            scope(farmId),
            created.id,
            UpsertField(farmId, "Talhão Sul", BigDecimal("12"), "Milho", "DKB", polygon),
        )
        assertEquals(farmId, patched.farmId)
        assertEquals("Talhão Sul", patched.name)
        assertEquals(BigDecimal("12"), patched.areaHa)
        assertEquals("Milho", patched.crop)
        assertEquals("DKB", patched.variety)
    }

    @Test
    fun patchFieldRejectsFarmMove() {
        val farmA = UUID.randomUUID()
        val farmB = UUID.randomUUID()
        lateinit var saved: FieldEntity
        every { farms.existsById(farmA) } returns true
        every { fields.save(any()) } answers { firstArg<FieldEntity>().also { saved = it } }
        every { fields.findById(any()) } answers { Optional.of(saved) }
        val polygon =
            """{"type":"Polygon","coordinates":[[[-50.92,-17.79],[-50.88,-17.79],[-50.88,-17.75],[-50.92,-17.75],[-50.92,-17.79]]]}"""
        val created = svc.createField(
            scope(farmA),
            UpsertField(farmA, "Talhão Norte", BigDecimal("10"), "Soja", null, polygon),
        )
        val ex = assertThrows(ConflictException::class.java) {
            svc.patchField(
                AccessScope(DemoTenant.ID, setOf(farmA, farmB), "ADMIN"),
                created.id,
                UpsertField(farmB, "Talhão Norte", BigDecimal("10"), "Soja", null, polygon),
            )
        }
        assertEquals("FIELD_FARM_IMMUTABLE", ex.code)
        assertEquals(farmA, saved.farmId)
    }

    @Test
    fun deleteFarmRemovesFarmAndFields() {
        val farmId = UUID.randomUUID()
        every { farms.existsById(farmId) } returns true
        every { fields.save(any()) } answers { firstArg<FieldEntity>() }
        val polygon =
            """{"type":"Polygon","coordinates":[[[-50.92,-17.79],[-50.88,-17.79],[-50.88,-17.75],[-50.92,-17.75],[-50.92,-17.79]]]}"""
        svc.createField(
            scope(farmId),
            UpsertField(farmId, "Talhão Norte", BigDecimal("10"), "Soja", null, polygon),
        )
        every { seasons.deleteByFarmId(farmId) } returns 0
        every { fields.deleteByFarmId(farmId) } returns 1
        every { farms.deleteById(farmId) } returns Unit
        svc.deleteFarm(scope(farmId), farmId)
        verify { farms.deleteById(farmId) }
        verify { fields.deleteByFarmId(farmId) }
        verify { seasons.deleteByFarmId(farmId) }
    }

    @Test
    fun deleteFailureRestoresMembershipAndKeepsTheGrantError() {
        val userId = UUID.randomUUID()
        val farmId = UUID.randomUUID()
        every { farms.existsById(farmId) } returns true
        every { seasons.deleteByFarmId(farmId) } throws IllegalStateException("db down")
        every { memberships.grant(userId, farmId) } throws IllegalStateException("auth down")

        val ex = assertThrows(IllegalStateException::class.java) {
            svc.deleteFarm(scope(farmId, userId), farmId)
        }
        assertEquals("db down", ex.message)
        assertEquals("auth down", ex.suppressed.single().message)
        verify(exactly = 0) { fields.deleteByFarmId(farmId) }
        verify(exactly = 0) { farms.deleteById(farmId) }
    }
}
