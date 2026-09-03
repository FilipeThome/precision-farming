package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FinancePnlDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.computeDashboardKpis
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardKpisTest {
    @Test
    fun matchesWebDashboardMath() {
        val kpis = computeDashboardKpis(
            farms = listOf(
                FarmDto("f1", "A", "X"),
                FarmDto("f2", "B", "Y"),
            ),
            machines = listOf(
                MachineDto("m1", "T1", "OPERATING", "TRACTOR"),
                MachineDto("m2", "T2", "IDLE", "SPRAYER"),
                MachineDto("m3", "T3", "DOWN", "HARVESTER"),
            ),
            operations = listOf(
                OperationDto("o1", "PLANT", "COMPLETED"),
                OperationDto("o2", "SPRAY", "IN_PROGRESS"),
                OperationDto("o3", "HARVEST", "COMPLETED"),
                OperationDto("o4", "TILL", "PLANNED"),
            ),
            alerts = listOf(
                AlertDto("a1", "Storm", "CRITICAL", "OPEN"),
                AlertDto("a2", "Low fuel", "WARNING", "OPEN"),
                AlertDto("a3", "Old", "CRITICAL", "ACKED"),
            ),
            pnl = listOf(
                FinancePnlDto(id = "p1", revenue = 100.0, cost = 40.0, margin = 60.0),
                FinancePnlDto(id = "p2", revenue = 10.0, cost = 30.0, margin = -20.0),
            ),
        )
        assertEquals(2, kpis.farmCount)
        assertEquals(3, kpis.machineCount)
        assertEquals(4, kpis.operationCount)
        assertEquals(3, kpis.alertCount)
        assertEquals(2, kpis.completedOps)
        assertEquals(50, kpis.opsProgressPct)
        assertEquals(1, kpis.criticalAlerts)
        assertEquals(2, kpis.availableMachines)
        assertEquals(67, kpis.fleetPct)
        assertEquals(1, kpis.negativeMargins)
        assertEquals(110.0, kpis.pnlRevenue, 0.0)
        assertEquals(70.0, kpis.pnlCost, 0.0)
        assertEquals(40.0, kpis.pnlMargin, 0.0)
    }

    @Test
    fun emptyInputsAreZero() {
        val kpis = computeDashboardKpis(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(0, kpis.opsProgressPct)
        assertEquals(0, kpis.fleetPct)
        assertEquals(0, kpis.criticalAlerts)
        assertEquals(true, kpis.isEmpty)
    }
}
