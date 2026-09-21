package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.canCreateFarm
import com.precisionfarming.mobile.data.canManageFarmOps
import com.precisionfarming.mobile.data.canWriteFleet
import com.precisionfarming.mobile.data.canWriteMasterData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesTest {
    @Test
    fun adminAndFarmManagerCanManage() {
        assertTrue(canManageFarmOps("ADMIN"))
        assertTrue(canManageFarmOps("FARM_MANAGER"))
        assertTrue(canManageFarmOps("admin"))
        assertFalse(canManageFarmOps("OPERATOR"))
        assertFalse(canManageFarmOps(null))
        assertFalse(canManageFarmOps(""))
    }

    @Test
    fun cadastroGates() {
        assertTrue(canCreateFarm("ADMIN"))
        assertFalse(canCreateFarm("FARM_MANAGER"))
        assertTrue(canWriteMasterData("FARM_MANAGER"))
        assertFalse(canWriteMasterData("OPERATOR"))
        assertTrue(canWriteFleet("MAINTENANCE"))
        assertFalse(canWriteFleet("OPERATOR"))
    }
}
