package com.precisionfarming.alert

import com.precisionfarming.alert.application.demo.DemoAlerts
import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlertDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("alert-001"), DemoIds.uuid("alert-001"))
    }

    @Test
    fun demoAlertsAreDetailedAndDeterministic() {
        val rows = DemoAlerts.rows()

        assertEquals(20, rows.size)
        assertEquals(rows.size, rows.map { it.id }.distinct().size)
        assertTrue(rows.any { it.message.contains("108°C") })
        assertTrue(rows.any { it.message.contains("22 mm") })
        assertTrue(rows.any { it.message.contains("11%") })
        assertTrue(rows.any { it.message.contains("Fazenda Boa Vista") })
        assertTrue(rows.any { it.message.contains("Trator 01") })
        assertTrue(rows.zipWithNext().all { (a, b) -> a.createdAt.isAfter(b.createdAt) })
    }
}
