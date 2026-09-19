package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.canManageFarmOps
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
}
