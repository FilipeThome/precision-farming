package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.EntityNames
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.FinancePnlDto
import com.precisionfarming.mobile.data.HarvestPlanDto
import com.precisionfarming.mobile.data.LogisticsLoadDto
import com.precisionfarming.mobile.data.MeDto
import com.precisionfarming.mobile.data.StorageLotDto
import com.precisionfarming.mobile.data.StorageUnitDto
import com.precisionfarming.mobile.data.YieldRecordDto
import com.precisionfarming.mobile.data.canManageFarmOps
import com.precisionfarming.mobile.data.dispatchLoad
import com.precisionfarming.mobile.data.expectedTons
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.financePnl
import com.precisionfarming.mobile.data.flowStages
import com.precisionfarming.mobile.data.formatTons
import com.precisionfarming.mobile.data.harvestPlans
import com.precisionfarming.mobile.data.harvestYield
import com.precisionfarming.mobile.data.harvestedTons
import com.precisionfarming.mobile.data.loadsSummary
import com.precisionfarming.mobile.data.logisticsLoads
import com.precisionfarming.mobile.data.marginRows
import com.precisionfarming.mobile.data.me
import com.precisionfarming.mobile.data.qualityBreakdown
import com.precisionfarming.mobile.data.storageLots
import com.precisionfarming.mobile.data.storageOccupancyTotal
import com.precisionfarming.mobile.data.storageUnits
import com.precisionfarming.mobile.data.unitOccupancyPct
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityCard
import com.precisionfarming.mobile.ui.components.KpiCard
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.ScreenHeader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class PostHarvestBundle(
    val plans: List<HarvestPlanDto>,
    val yields: List<YieldRecordDto>,
    val loads: List<LogisticsLoadDto>,
    val units: List<StorageUnitDto>,
    val lots: List<StorageLotDto>,
    val fields: List<FieldDto>,
    val pnl: List<FinancePnlDto>,
    val me: MeDto?,
    val meFailed: Boolean,
)

@Composable
fun PostHarvestTowerScreen(
    onBack: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    var state by remember { mutableStateOf<LoadState<PostHarvestBundle>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching {
                val farmId = FarmFilter.farmId.value
                coroutineScope {
                    val planJob = async { harvestPlans(farmId) }
                    val yieldJob = async { harvestYield(farmId) }
                    val loadJob = async { logisticsLoads(farmId) }
                    val unitJob = async { storageUnits(farmId) }
                    val lotJob = async { storageLots(farmId) }
                    val fieldJob = async { runCatching { fields(farmId) }.getOrDefault(emptyList()) }
                    val pnlJob = async { runCatching { financePnl(farmId) }.getOrDefault(emptyList()) }
                    val meJob = async { runCatching { me() } }
                    val meResult = meJob.await()
                    PostHarvestBundle(
                        plans = planJob.await(),
                        yields = yieldJob.await(),
                        loads = loadJob.await(),
                        units = unitJob.await(),
                        lots = lotJob.await(),
                        fields = fieldJob.await(),
                        pnl = pnlJob.await(),
                        me = meResult.getOrNull(),
                        meFailed = meResult.isFailure,
                    )
                }
            }.fold(
                onSuccess = { LoadState.Ok(listOf(it)) },
                onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
            )
        }
    }

    LaunchedEffect(LocalFarmId.current, LocalAppLocale.current) { reload() }

    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader(S.t("postHarvest.title"), onBack)
        Text(S.t("postHarvest.description"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        msg?.let { Text(it) }

        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                val b = s.items.first()
                val canDispatch = canManageFarmOps(b.me?.role)
                val stages = flowStages(b.plans, b.loads, b.lots)
                val expected = expectedTons(b.plans, b.fields)
                val harvested = harvestedTons(b.yields)
                val loads = loadsSummary(b.loads)
                val occupancy = storageOccupancyTotal(b.units)
                val quality = qualityBreakdown(b.lots)
                val margins = marginRows(b.pnl)

                Text(S.t("postHarvest.flow.label"), style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stages.forEach { stage ->
                        FilterChip(
                            selected = stage.state.name == "NOW",
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    "${S.t("postHarvest.flow.${stage.id.name}")} · ${S.t("chain.state.${stage.state.name.lowercase()}")}" +
                                        if (stage.count > 0) " (${stage.count})" else "",
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

                KpiCard(
                    label = S.t("postHarvest.kpi.expected"),
                    value = "${formatTons(expected.tons)} t",
                    hint = if (expected.withArea == 0) {
                        S.t("postHarvest.kpi.noArea")
                    } else {
                        S.t("postHarvest.kpi.plansWithArea", "n" to expected.withArea.toString(), "m" to expected.total.toString())
                    },
                )
                KpiCard(
                    label = S.t("postHarvest.kpi.harvested"),
                    value = "${formatTons(harvested.tons)} t",
                    hint = if (harvested.withArea == 0) {
                        S.t("postHarvest.kpi.noYield")
                    } else {
                        S.t("postHarvest.kpi.recordsWithArea", "n" to harvested.withArea.toString(), "m" to harvested.total.toString())
                    },
                )
                KpiCard(
                    label = S.t("postHarvest.kpi.inTransit"),
                    value = loads.inTransit.toString(),
                    hint = S.t(
                        "postHarvest.kpi.loadsHint",
                        "queued" to loads.queued.toString(),
                        "delivered" to loads.delivered.toString(),
                        "tons" to formatTons(loads.tonsInTransit),
                    ),
                )
                KpiCard(
                    label = S.t("postHarvest.kpi.occupancy"),
                    value = occupancy.pct?.let { "$it%" } ?: "—",
                    hint = if (occupancy.pct == null) {
                        S.t("postHarvest.kpi.noStorage")
                    } else {
                        "${formatTons(occupancy.usedT)} / ${formatTons(occupancy.capacityT)} t"
                    },
                )

                Text(S.t("postHarvest.loads.title"), style = MaterialTheme.typography.titleMedium)
                if (b.loads.isEmpty()) {
                    Text(S.t("common.empty"))
                } else {
                    b.loads.forEach { load ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            EntityCard(
                                headline = load.truckPlate ?: load.id,
                                supporting = listOfNotNull(load.destination, load.tons?.let { "${it}t" }).joinToString(" · "),
                                status = DomainLabels.label(load.status),
                            )
                            val queued = load.status?.equals("QUEUED", ignoreCase = true) == true
                            if (queued && canDispatch) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            runCatching { dispatchLoad(load.id) }
                                                .onSuccess { msg = S.t("harvest.dispatchOk") }
                                                .onFailure { msg = it.message }
                                            reload()
                                        }
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text(S.t("harvest.dispatch")) }
                            }
                            if (queued && b.meFailed) {
                                Text(S.t("auth.meLoadError"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            if (queued && !b.meFailed && b.me != null && !canDispatch) {
                                Text(
                                    S.t("harvest.dispatchNoPermission"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text(S.t("postHarvest.silos.title"), style = MaterialTheme.typography.titleMedium)
                if (b.units.isEmpty()) {
                    Text(S.t("common.empty"))
                } else {
                    b.units.forEach { u ->
                        EntityCard(
                            headline = u.name ?: u.id,
                            supporting = listOfNotNull(
                                DomainLabels.label(u.type),
                                unitOccupancyPct(u)?.let { "$it%" },
                            ).joinToString(" · "),
                        )
                    }
                }

                Text(S.t("postHarvest.lots.title"), style = MaterialTheme.typography.titleMedium)
                Text(S.t("postHarvest.lots.count", "n" to b.lots.size.toString()), style = MaterialTheme.typography.labelMedium)
                if (quality.isEmpty()) {
                    Text(S.t("common.empty"))
                } else {
                    quality.forEach { q ->
                        EntityCard(
                            headline = DomainLabels.label(q.quality),
                            supporting = "${q.count} · ${q.tons} t",
                        )
                    }
                }

                Text(S.t("postHarvest.margin.title"), style = MaterialTheme.typography.titleMedium)
                if (margins.isEmpty()) {
                    Text(S.t("postHarvest.margin.empty"))
                } else {
                    margins.forEach { row ->
                        EntityCard(
                            headline = EntityNames.nameOf(row.fieldId) ?: row.fieldId,
                            supporting = listOfNotNull(row.currency, row.margin.toInt().toString()).joinToString(" "),
                        )
                    }
                }

                Button(
                    onClick = onOpenDetail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) { Text(S.t("postHarvest.seeAll")) }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
