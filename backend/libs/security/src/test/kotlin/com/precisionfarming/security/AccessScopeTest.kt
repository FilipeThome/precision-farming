package com.precisionfarming.security

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class AccessScopeTest {
    private val farm1 = DemoIds.uuid("farm-001")
    private val farm2 = DemoIds.uuid("farm-002")
    private val farm3 = DemoIds.uuid("farm-003")
    private val scope = AccessScope(
        tenantId = DemoTenant.ID,
        farmIds = setOf(farm1, farm2),
        role = "OPERATOR",
    )

    @Test
    fun resolveFarmsReturnsAllWhenRequestNull() {
        assertEquals(setOf(farm1, farm2), scope.resolveFarms(null))
    }

    @Test
    fun resolveFarmsReturnsSingleWhenInScope() {
        assertEquals(setOf(farm1), scope.resolveFarms(farm1))
    }

    @Test
    fun resolveFarmsRejectsOutOfScope() {
        assertThrows(ForbiddenException::class.java) { scope.resolveFarms(farm3) }
    }

    @Test
    fun requireFarmRejectsUnknownFarm() {
        val ex = assertThrows(ForbiddenException::class.java) { scope.requireFarm(UUID.randomUUID()) }
        assertEquals("FARM_SCOPE_DENIED", ex.code)
    }

    @Test
    fun requireEntityFarmAllowsScopedFarm() {
        scope.requireEntityFarm(farm2)
    }

    @Test
    fun requireFarmReadLooksLikeMissing() {
        val ex = assertThrows(NotFoundException::class.java) {
            scope.requireFarmRead(farm3, "ITEM_NOT_FOUND", "Item not found")
        }
        assertEquals("ITEM_NOT_FOUND", ex.code)
        assertEquals(404, ex.httpStatus)
    }
}
