package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.precisionfarming.mobile.data.DashboardKpis
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.alerts
import com.precisionfarming.mobile.data.computeDashboardKpis
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.data.financePnl
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.KpiCard
import com.precisionfarming.mobile.ui.components.LoadState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen() {
    var state by remember { mutableStateOf<LoadState<DashboardKpis>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching {
                val farmId = FarmFilter.farmId
                coroutineScope {
                    val farmJob = async { farms() }
                    val machineJob = async { machines(farmId) }
                    val opJob = async { operations(farmId) }
                    val alertJob = async { alerts(farmId) }
                    val pnlJob = async { financePnl(farmId) }
                    computeDashboardKpis(
                        farmJob.await(),
                        machineJob.await(),
                        opJob.await(),
                        alertJob.await(),
                        pnlJob.await(),
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(S.t("dashboard.title"), style = MaterialTheme.typography.titleLarge)
        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                val kpis = s.items.first()
                if (kpis.isEmpty) {
                    Text(S.t("common.empty"))
                } else {
                    KpiCard(S.t("dashboard.kpi.farms"), kpis.farmCount.toString())
                    KpiCard(S.t("dashboard.kpi.machines"), kpis.machineCount.toString())
                    KpiCard(S.t("dashboard.kpi.operations"), kpis.operationCount.toString())
                    KpiCard(S.t("dashboard.kpi.alerts"), kpis.alertCount.toString())
                    KpiCard(
                        S.t("dashboard.kpi.opsProgress"),
                        "${kpis.opsProgressPct}%",
                        "${kpis.completedOps} / ${kpis.operationCount} · ${S.t("dashboard.kpi.opsProgressHint")}",
                    )
                    KpiCard(
                        S.t("dashboard.kpi.criticalAlerts"),
                        kpis.criticalAlerts.toString(),
                        S.t("dashboard.kpi.criticalAlertsHint"),
                    )
                    KpiCard(
                        S.t("dashboard.kpi.fleetAvailability"),
                        "${kpis.fleetPct}%",
                        S.t("dashboard.kpi.fleetAvailabilityHint"),
                    )
                    KpiCard(
                        S.t("dashboard.kpi.marginRisk"),
                        kpis.negativeMargins.toString(),
                        S.t("dashboard.kpi.marginRiskHint"),
                    )
                    GroupLines(S.t("dashboard.group.ops"), kpis.opsByStatus)
                    GroupLines(S.t("dashboard.group.alerts"), kpis.alertsBySeverity)
                    GroupLines(S.t("dashboard.group.fleet"), kpis.fleetByStatus)
                    Text(S.t("dashboard.group.pnl"), style = MaterialTheme.typography.titleMedium)
                    Text("${S.t("dashboard.pnl.revenue")} · ${kpis.pnlRevenue}")
                    Text("${S.t("dashboard.pnl.cost")} · ${kpis.pnlCost}")
                    Text("${S.t("dashboard.pnl.margin")} · ${kpis.pnlMargin}")
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}

@Composable
private fun GroupLines(title: String, rows: List<Pair<String, Int>>) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    if (rows.isEmpty()) {
        Text(S.t("common.empty"))
    } else {
        rows.forEach { (name, count) -> Text("$name · $count") }
    }
}
