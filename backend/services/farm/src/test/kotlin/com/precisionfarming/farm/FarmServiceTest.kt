package com.precisionfarming.farm

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
import com.precisionfarming.security.DemoFieldFarms
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.security.UserFarmGrants
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class FarmServiceTest {
    private val farms = mockk<FarmJpaRepository>()
    private val fields = mockk<FieldJpaRepository>()
    private val seasons = mockk<SeasonJpaRepository>()
    private val memberships = mockk<AuthMembershipClient>(relaxed = true)
    private val svc = FarmService(farms, fields, seasons, memberships)

    private fun scope(farmId: UUID, userId: UUID? = null) =
        AccessScope(DemoTenant.ID, setOf(farmId), "ADMIN", userId)

    @Test
    fun createFarmAddsFarmToCreatorGrants() {
        val userId = UUID.randomUUID()
        every { farms.save(any()) } answers { firstArg<FarmEntity>() }

        val dto = svc.createFarm(
            scope(UUID.randomUUID(), userId),
            UpsertFarm("Nova", "MS", BigDecimal.TEN, "America/Campo_Grande"),
        )
        try {
            assertTrue(UserFarmGrants.farmIds(userId).contains(dto.id))
        } finally {
            UserFarmGrants.revoke(dto.id, userId)
        }
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
    fun patchFieldUpdatesDemoFieldFarmMap() {
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
        try {
            assertEquals(farmA, DemoFieldFarms.farmId(created.id))
            val patched = svc.patchField(
                AccessScope(DemoTenant.ID, setOf(farmA, farmB), "ADMIN"),
                created.id,
                UpsertField(farmB, "Talhão Norte", BigDecimal("10"), "Soja", null, polygon),
            )
            assertEquals(farmB, patched.farmId)
            assertEquals(farmB, DemoFieldFarms.farmId(created.id))
        } finally {
            DemoFieldFarms.unregister(created.id)
        }
    }

    @Test
    fun deleteFarmUnregistersItsFields() {
        val farmId = UUID.randomUUID()
        every { farms.existsById(farmId) } returns true
        every { fields.save(any()) } answers { firstArg<FieldEntity>() }
        val polygon =
            """{"type":"Polygon","coordinates":[[[-50.92,-17.79],[-50.88,-17.79],[-50.88,-17.75],[-50.92,-17.75],[-50.92,-17.79]]]}"""
        val created = svc.createField(
            scope(farmId),
            UpsertField(farmId, "Talhão Norte", BigDecimal("10"), "Soja", null, polygon),
        )
        val row = mockk<FieldEntity>()
        every { row.id } returns created.id
        every { fields.findByFarmId(farmId) } returns listOf(row)
        every { seasons.deleteByFarmId(farmId) } returns 0
        every { fields.deleteByFarmId(farmId) } returns 1
        every { farms.deleteById(farmId) } returns Unit
        try {
            assertEquals(farmId, DemoFieldFarms.farmId(created.id))
            svc.deleteFarm(scope(farmId), farmId)
            assertEquals(null, DemoFieldFarms.farmId(created.id))
        } finally {
            DemoFieldFarms.unregister(created.id)
        }
    }
}
