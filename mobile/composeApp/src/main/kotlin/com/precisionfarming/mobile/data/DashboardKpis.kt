package com.precisionfarming.mobile.data

data class DashboardKpis(
    val farmCount: Int,
    val machineCount: Int,
    val operationCount: Int,
    val alertCount: Int,
    val completedOps: Int,
    val opsProgressPct: Int,
    val criticalAlerts: Int,
    val availableMachines: Int,
    val fleetPct: Int,
    val negativeMargins: Int,
    val opsByStatus: List<Pair<String, Int>>,
    val alertsBySeverity: List<Pair<String, Int>>,
    val fleetByStatus: List<Pair<String, Int>>,
    val pnlRowCount: Int,
    val pnlRevenue: Double,
    val pnlCost: Double,
    val pnlMargin: Double,
) {
    val isEmpty: Boolean
        get() = farmCount + machineCount + operationCount + alertCount + pnlRowCount == 0
}

/** Same formulas as web DashboardPage.tsx. */
fun computeDashboardKpis(
    farms: List<FarmDto>,
    machines: List<MachineDto>,
    operations: List<OperationDto>,
    alerts: List<AlertDto>,
    pnl: List<FinancePnlDto>,
): DashboardKpis {
    val farmCount = farms.size
    val machineCount = machines.size
    val operationCount = operations.size
    val alertCount = alerts.size
    val completedOps = operations.count { it.status == "COMPLETED" }
    val opsProgressPct =
        if (operationCount == 0) 0 else kotlin.math.round(completedOps * 100.0 / operationCount).toInt()
    val criticalAlerts = alerts.count { it.severity == "CRITICAL" && it.status == "OPEN" }
    val availableMachines = machines.count { it.status == "OPERATING" || it.status == "IDLE" }
    val fleetPct =
        if (machineCount == 0) 0 else kotlin.math.round(availableMachines * 100.0 / machineCount).toInt()
    val negativeMargins = pnl.count { (it.margin ?: 0.0) < 0 }
    return DashboardKpis(
        farmCount = farmCount,
        machineCount = machineCount,
        operationCount = operationCount,
        alertCount = alertCount,
        completedOps = completedOps,
        opsProgressPct = opsProgressPct,
        criticalAlerts = criticalAlerts,
        availableMachines = availableMachines,
        fleetPct = fleetPct,
        negativeMargins = negativeMargins,
        opsByStatus = countBy(operations.map { it.status }),
        alertsBySeverity = countBy(alerts.map { it.severity }),
        fleetByStatus = countBy(machines.map { it.status }),
        pnlRowCount = pnl.size,
        pnlRevenue = pnl.sumOf { it.revenue ?: 0.0 },
        pnlCost = pnl.sumOf { it.cost ?: 0.0 },
        pnlMargin = pnl.sumOf { it.margin ?: 0.0 },
    )
}

private fun countBy(values: List<String>): List<Pair<String, Int>> =
    values.groupingBy { it }.eachCount().toList()
