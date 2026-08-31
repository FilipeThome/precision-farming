package com.precisionfarming.farm

import com.precisionfarming.common.DomainException
import com.precisionfarming.farm.application.FarmService
import com.precisionfarming.farm.application.UpsertField
import com.precisionfarming.farm.infrastructure.FarmJpaRepository
import com.precisionfarming.farm.infrastructure.FieldEntity
import com.precisionfarming.farm.infrastructure.FieldJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class FarmServiceTest {
    private val farms = mockk<FarmJpaRepository>()
    private val fields = mockk<FieldJpaRepository>()
    private val svc = FarmService(farms, fields)

    @Test
    fun createFieldPersistsParsedGeoJsonCoordinates() {
        val farmId = UUID.randomUUID()
        every { farms.existsById(farmId) } returns true
        every { fields.save(any()) } answers { firstArg<FieldEntity>() }

        val dto = svc.createField(
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
}
