package com.precisionfarming.mobile.ui.inspectors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.InventoryItemDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.MachineMetricsDto
import com.precisionfarming.mobile.data.MachineWorkSummaryDto
import com.precisionfarming.mobile.data.formatNumber
import com.precisionfarming.mobile.data.formatWhen
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.data.machineFreshness
import com.precisionfarming.mobile.data.machineMetrics
import com.precisionfarming.mobile.data.machineWorkSummary
import com.precisionfarming.mobile.data.rollingWeekIsoRange
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.InspectorKpiItem
import com.precisionfarming.mobile.ui.components.InspectorKpis

@Composable
fun MachineInspector(machine: MachineDto, modifier: Modifier = Modifier) {
    var metrics by remember { mutableStateOf<MachineMetricsDto?>(null) }
    var work by remember { mutableStateOf<MachineWorkSummaryDto?>(null) }
    var items by remember { mutableStateOf<List<InventoryItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(machine.id, FarmFilter.farmId) {
        loading = true
        error = null
        val range = rollingWeekIsoRange()
        val metricsResult = runCatching { machineMetrics(machine.id) }
        val workResult = runCatching { machineWorkSummary(machine.id, range.first, range.second) }
        items = runCatching { inventory(FarmFilter.farmId) }.getOrDefault(emptyList())
        metrics = metricsResult.getOrNull()
        work = workResult.getOrNull()
        error = metricsResult.exceptionOrNull()?.message ?: workResult.exceptionOrNull()?.message
        loading = false
    }
    val source = machineFreshness(metrics?.lastObservedAt)
    val names = items.associate { it.id to (it.name ?: it.id) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(DomainLabels.label(machine.status))
        source?.let { key ->
            val label = when (key) {
                "LIVE" -> S.t("inspector.source.live")
                "STALE" -> S.t("inspector.source.stale")
                else -> S.t("inspector.source.demo")
            }
            Text(label)
        }
        Text(
            listOfNotNull(
                DomainLabels.label(machine.type),
                machine.manufacturer,
                machine.model,
            ).joinToString(" · "),
        )
        Text(
            metrics?.lastObservedAt?.let { S.t("inspector.lastSeen", "when" to formatWhen(it)) }
                ?: S.t("inspector.lastSeenUnknown"),
        )
        if (loading) Text(S.t("common.loading"))
        error?.let { Text("${S.t("common.error")}: $it") }
        InspectorKpis(
            listOf(
                InspectorKpiItem(
                    S.t("machines.kpi.engineHours"),
                    metrics?.let { "${formatNumber(it.engineHours)} h" } ?: "—",
                ),
                InspectorKpiItem(
                    S.t("machines.kpi.areaCovered"),
                    work?.let { "${formatNumber(it.areaHa)} ha" } ?: "—",
                ),
                InspectorKpiItem(
                    S.t("machines.kpi.inputsUsed"),
                    work?.inputs?.sumOf { it.quantity }?.let { formatNumber(it) } ?: "—",
                ),
            ),
        )
        val hours = metrics?.days.orEmpty()
        if (hours.isNotEmpty()) {
            Text(S.t("charts.hoursPerDay"))
            hours.takeLast(7).forEach { day ->
                Text("${day.day.takeLast(5)} · ${formatNumber(day.hours)} h · ${S.t("charts.fuel")} ${formatNumber(day.fuel)}")
            }
        }
        val areaDays = work?.days.orEmpty()
        if (areaDays.isNotEmpty()) {
            Text(S.t("charts.areaPerDay"))
            areaDays.takeLast(7).forEach { day ->
                Text("${day.day.takeLast(5)} · ${formatNumber(day.areaHa)} ha")
            }
        }
        val inputs = work?.inputs.orEmpty()
        if (inputs.isNotEmpty()) {
            Text(S.t("charts.inputsByProduct"))
            inputs.forEach { row ->
                Text("${names[row.itemId] ?: DomainLabels.label(row.itemId)} · ${formatNumber(row.quantity)}")
            }
        }
    }
}
