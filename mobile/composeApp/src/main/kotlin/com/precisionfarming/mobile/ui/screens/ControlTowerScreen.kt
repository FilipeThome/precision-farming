package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.DecisionItem
import com.precisionfarming.mobile.data.DecisionSources
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.InspectNav
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.WeatherWindowDto
import com.precisionfarming.mobile.data.actionableDecisions
import com.precisionfarming.mobile.data.alerts
import com.precisionfarming.mobile.data.compactTodayTimeline
import com.precisionfarming.mobile.data.favorableWindowUntil
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.fleetAvailability
import com.precisionfarming.mobile.data.insights
import com.precisionfarming.mobile.data.irrigationRecommendations
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.needsHumanReview
import com.precisionfarming.mobile.data.openCriticalAlerts
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.data.prescriptions
import com.precisionfarming.mobile.data.recommendations
import com.precisionfarming.mobile.data.sortByPriority
import com.precisionfarming.mobile.data.toDecisionItems
import com.precisionfarming.mobile.data.traceabilityRatio
import com.precisionfarming.mobile.data.weatherWindows
import com.precisionfarming.mobile.data.withinWindowRatio
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityCard
import com.precisionfarming.mobile.ui.components.FieldStatusList
import com.precisionfarming.mobile.ui.components.KpiCard
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.ScreenHeader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

private data class TowerBundle(
    val fields: List<FieldDto>,
    val operations: List<OperationDto>,
    val alerts: List<AlertDto>,
    val machines: List<MachineDto>,
    val windows: List<WeatherWindowDto>,
    val decisions: List<DecisionItem>,
    val partialError: Boolean,
)

@Composable
fun ControlTowerScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var state by remember { mutableStateOf<LoadState<TowerBundle>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching {
                val farmId = FarmFilter.farmId
                coroutineScope {
                    val fieldJob = async { runCatching { fields(farmId) } }
                    val opJob = async { runCatching { operations(farmId) } }
                    val alertJob = async { runCatching { alerts(farmId) } }
                    val machineJob = async { runCatching { machines(farmId) } }
                    val windowJob = async { runCatching { weatherWindows(farmId) } }
                    val rxJob = async { runCatching { prescriptions(farmId) } }
                    val irrJob = async { runCatching { irrigationRecommendations(farmId) } }
                    val agroJob = async { runCatching { recommendations(farmId) } }
                    val insightJob = async { runCatching { insights(farmId) } }
                    val field = fieldJob.await()
                    val op = opJob.await()
                    val alert = alertJob.await()
                    val machine = machineJob.await()
                    val window = windowJob.await()
                    val rx = rxJob.await()
                    val irr = irrJob.await()
                    val agro = agroJob.await()
                    val insight = insightJob.await()
                    val fieldList = field.getOrDefault(emptyList())
                    val partial = listOf(field, op, alert, machine, window, rx, irr, agro, insight).any { it.isFailure }
                    if (op.isFailure && alert.isFailure && machine.isFailure && field.isFailure) {
                        error(op.exceptionOrNull()?.message ?: S.t("common.error"))
                    }
                    TowerBundle(
                        fields = fieldList,
                        operations = op.getOrDefault(emptyList()),
                        alerts = alert.getOrDefault(emptyList()),
                        machines = machine.getOrDefault(emptyList()),
                        windows = window.getOrDefault(emptyList()),
                        decisions = toDecisionItems(
                            DecisionSources(
                                prescriptions = rx.getOrDefault(emptyList()),
                                irrigation = irr.getOrDefault(emptyList()),
                                agronomy = agro.getOrDefault(emptyList()),
                                insights = insight.getOrDefault(emptyList()),
                                fields = fieldList,
                            ),
                        ),
                        partialError = partial,
                    )
                }
            }.fold(
                onSuccess = { LoadState.Ok(listOf(it)) },
                onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
            )
        }
    }

    LaunchedEffect(FarmFilter.farmId, LocaleStore.locale) { reload() }

    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader(S.t("tower.title"), onBack)
        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                val b = s.items.first()
                if (b.partialError) {
                    Text(
                        S.t("decisions.partialError"),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val within = withinWindowRatio(b.operations)
                val trace = traceabilityRatio(b.operations)
                val critical = openCriticalAlerts(b.alerts)
                val fleet = fleetAvailability(b.machines)
                val favorableUntil = favorableWindowUntil(b.windows)

                favorableUntil?.let {
                    Text(
                        S.t("tower.favorableUntil", "when" to it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                KpiCard(
                    label = S.t("tower.northStar.label"),
                    value = within.pct?.let { "$it%" } ?: "—",
                    hint = within.pct?.let {
                        S.t("tower.ratioHint", "n" to within.numerator.toString(), "m" to within.denominator.toString())
                    } ?: S.t("tower.northStar.emptyHint"),
                )
                KpiCard(
                    label = S.t("tower.kpi.traceability"),
                    value = trace.pct?.let { "$it%" } ?: "—",
                    hint = S.t("tower.kpi.traceabilityHint", "n" to trace.numerator.toString(), "m" to trace.denominator.toString()),
                )
                KpiCard(
                    label = S.t("tower.kpi.criticalAlerts"),
                    value = critical.toString(),
                    hint = S.t("tower.kpi.criticalAlertsHint"),
                    onClick = { onOpen(InspectNav.href(InspectNav.ALERTS, severity = "CRITICAL")) },
                )
                KpiCard(
                    label = S.t("tower.kpi.fleet"),
                    value = fleet.pct?.let { "$it%" } ?: "—",
                    hint = if (fleet.denominator == 0) {
                        S.t("tower.kpi.noMachines")
                    } else {
                        S.t("tower.kpi.fleetHint", "n" to fleet.numerator.toString(), "m" to fleet.denominator.toString())
                    },
                    onClick = { onOpen(InspectNav.href(InspectNav.MACHINES)) },
                )

                ActionQueueSection(
                    decisions = b.decisions,
                    alerts = b.alerts,
                    onOpen = onOpen,
                )

                FieldStatusList(fields = b.fields, operations = b.operations)

                Text(S.t("tower.timeline.title"), style = MaterialTheme.typography.titleMedium)
                val timeline = compactTodayTimeline(b.operations, Instant.now())
                if (timeline.isEmpty()) {
                    Text(S.t("tower.timeline.empty"), style = MaterialTheme.typography.bodyMedium)
                } else {
                    timeline.take(8).forEach { row ->
                        EntityCard(
                            headline = DomainLabels.label(row.operation.type),
                            supporting = listOfNotNull(
                                row.operation.plannedStart,
                                DomainLabels.label(row.operation.status),
                            ).joinToString(" · "),
                            onClick = { onOpen(InspectNav.href(InspectNav.OPS, selected = row.operation.id)) },
                        )
                    }
                }

                Text(S.t("tower.fleet.title"), style = MaterialTheme.typography.titleMedium)
                Text(
                    S.t("tower.fleet.available", "n" to fleet.numerator.toString(), "m" to fleet.denominator.toString()),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (b.machines.isEmpty()) {
                    Text(S.t("tower.fleet.empty"))
                } else {
                    b.machines.take(8).forEach { m ->
                        EntityCard(
                            headline = m.name,
                            supporting = DomainLabels.label(m.type),
                            status = DomainLabels.label(m.status),
                            onClick = { onOpen(InspectNav.href(InspectNav.MACHINES, selected = m.id)) },
                        )
                    }
                    TextButton(onClick = { onOpen(InspectNav.href(InspectNav.MACHINES)) }) {
                        Text(S.t("tower.fleet.seeAll"))
                    }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}

@Composable
private fun ActionQueueSection(
    decisions: List<DecisionItem>,
    alerts: List<AlertDto>,
    onOpen: (String) -> Unit,
) {
    val decisionEntries = sortByPriority(actionableDecisions(decisions))
    val alertEntries = alerts
        .filter { it.status.equals("OPEN", ignoreCase = true) && it.severity.equals("CRITICAL", ignoreCase = true) }
        .sortedByDescending { it.createdAt.orEmpty() }
    val count = alertEntries.size + decisionEntries.size

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(S.t("tower.queue.title"), style = MaterialTheme.typography.titleMedium)
        Text(S.t("tower.queue.count", "n" to count.toString()), style = MaterialTheme.typography.labelMedium)
        if (count == 0) {
            Text(S.t("tower.queue.empty"), style = MaterialTheme.typography.bodyMedium)
        }
        alertEntries.forEach { alert ->
            EntityCard(
                headline = DomainLabels.label(alert.title),
                supporting = alert.message.orEmpty(),
                status = S.t("tower.queue.tag.critical"),
                onClick = { onOpen(InspectNav.href(InspectNav.ALERTS, selected = alert.id, severity = "CRITICAL")) },
            )
        }
        decisionEntries.forEach { item ->
            val tag = when {
                needsHumanReview(item) -> S.t("tower.queue.tag.review")
                item.source.name == "IRRIGATION" -> S.t("tower.queue.tag.irrigation")
                else -> S.t("tower.queue.tag.approval")
            }
            EntityCard(
                headline = DomainLabels.label(item.title),
                supporting = listOfNotNull(
                    S.t("decisions.source.${item.source.name}"),
                    item.quantity?.let { "${it.value} ${it.unit}" },
                ).joinToString(" · "),
                status = tag,
                onClick = { onOpen(InspectNav.href(InspectNav.DECISIONS, selected = item.id)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            )
        }
    }
}
