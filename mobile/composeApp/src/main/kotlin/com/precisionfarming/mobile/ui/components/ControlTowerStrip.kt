package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.InspectNav
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S


@Composable
fun ControlTowerStrip(
    alerts: List<AlertDto>,
    machines: List<MachineDto>,
    operations: List<OperationDto>,
    onOpen: (route: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openAlerts = alerts.filter { it.status.equals("OPEN", ignoreCase = true) }
        .sortedByDescending { it.createdAt.orEmpty() }
        .take(5)
    val fleet = machines.filter { it.status == "OPERATING" || it.status == "IDLE" }
        .sortedBy { if (it.status == "OPERATING") 0 else 1 }
        .take(5)
    val running = operations.filter { it.status.equals("IN_PROGRESS", ignoreCase = true) }.take(5)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TowerColumn(S.t("dashboard.tower.alerts"), openAlerts.isEmpty()) {
            openAlerts.forEach { alert ->
                TowerChip(
                    title = DomainLabels.label(alert.title),
                    status = DomainLabels.label(alert.severity),
                    onClick = {
                        FarmFilter.apply(alert.farmId)
                        onOpen(InspectNav.href(InspectNav.ALERTS, selected = alert.id))
                    },
                )
            }
        }
        TowerColumn(S.t("dashboard.tower.fleet"), fleet.isEmpty()) {
            fleet.forEach { machine ->
                TowerChip(
                    title = DomainLabels.label(machine.name),
                    status = DomainLabels.label(machine.status),
                    onClick = {
                        FarmFilter.apply(machine.farmId)
                        onOpen(InspectNav.href(InspectNav.MACHINES, selected = machine.id))
                    },
                )
            }
        }
        TowerColumn(S.t("dashboard.tower.ops"), running.isEmpty()) {
            running.forEach { op ->
                TowerChip(
                    title = DomainLabels.label(op.type),
                    status = DomainLabels.label(op.status),
                    onClick = {
                        FarmFilter.apply(op.farmId)
                        onOpen(InspectNav.href(InspectNav.OPS, selected = op.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun TowerColumn(title: String, empty: Boolean, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (empty) {
            Text(S.t("dashboard.tower.empty"), style = MaterialTheme.typography.bodyMedium)
        } else {
            content()
        }
    }
}

@Composable
private fun TowerChip(title: String, status: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { role = Role.Button },
    ) {
        Text("$title · $status", modifier = Modifier.padding(vertical = 4.dp))
    }
}
