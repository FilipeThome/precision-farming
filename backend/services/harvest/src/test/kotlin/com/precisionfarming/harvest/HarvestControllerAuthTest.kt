package com.precisionfarming.harvest

import com.precisionfarming.harvest.api.HarvestController
import com.precisionfarming.harvest.application.CreateHarvestPlan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize

class HarvestControllerAuthTest {
    @Test
    fun createPlanRequiresAdminOrFarmManager() {
        val method = HarvestController::class.java.getMethod("createPlan", CreateHarvestPlan::class.java)
        val auth = method.getAnnotation(PreAuthorize::class.java)
        assertNotNull(auth)
        assertEquals("hasAnyRole('ADMIN','FARM_MANAGER')", auth.value)
    }
}
