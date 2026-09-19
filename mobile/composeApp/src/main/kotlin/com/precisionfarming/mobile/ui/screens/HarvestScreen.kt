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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.HarvestPlanDto
import com.precisionfarming.mobile.data.LogisticsLoadDto
import com.precisionfarming.mobile.data.MeDto
import com.precisionfarming.mobile.data.StorageLotDto
import com.precisionfarming.mobile.data.StorageUnitDto
import com.precisionfarming.mobile.data.YieldRecordDto
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.data.canManageFarmOps
import com.precisionfarming.mobile.data.dispatchLoad
import com.precisionfarming.mobile.data.harvestPlans
import com.precisionfarming.mobile.data.harvestYield
import com.precisionfarming.mobile.data.logisticsLoads
import com.precisionfarming.mobile.data.me
import com.precisionfarming.mobile.data.storageLots
import com.precisionfarming.mobile.data.storageUnits
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.DetailSheet
import com.precisionfarming.mobile.ui.components.EntityCard
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import com.precisionfarming.mobile.ui.inspectors.HarvestPlanInspector
import com.precisionfarming.mobile.ui.inspectors.StorageLotInspector
import kotlinx.coroutines.launch
import com.precisionfarming.mobile.ui.components.ScreenHeader

private enum class HarvestTab { PLANS, YIELD, LOGISTICS, STORAGE }

@Composable
fun HarvestScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var storageSub by remember { mutableIntStateOf(0) }
    var plans by remember { mutableStateOf<LoadState<HarvestPlanDto>>(LoadState.Loading) }
    var yieldState by remember { mutableStateOf<LoadState<YieldRecordDto>>(LoadState.Loading) }
    var loads by remember { mutableStateOf<LoadState<LogisticsLoadDto>>(LoadState.Loading) }
    var units by remember { mutableStateOf<LoadState<StorageUnitDto>>(LoadState.Loading) }
    var lots by remember { mutableStateOf<LoadState<StorageLotDto>>(LoadState.Loading) }
    var meDto by remember { mutableStateOf<MeDto?>(null) }
    var meFailed by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val tabs = HarvestTab.entries
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId
            val meResult = runCatching { me() }
            meDto = meResult.getOrNull()
            meFailed = meResult.isFailure
            when (tabs[tab]) {
                HarvestTab.PLANS -> {
                    if (plans !is LoadState.Ok) plans = LoadState.Loading
                    plans = runCatching { harvestPlans(farmId) }.toLoadState()
                    yieldState = runCatching { harvestYield(farmId) }.toLoadState()
                }
                HarvestTab.YIELD -> {
                    if (yieldState !is LoadState.Ok) yieldState = LoadState.Loading
                    yieldState = runCatching { harvestYield(farmId) }.toLoadState()
                }
                HarvestTab.LOGISTICS -> {
                    if (loads !is LoadState.Ok) loads = LoadState.Loading
                    loads = runCatching { logisticsLoads(farmId) }.toLoadState()
                }
                HarvestTab.STORAGE -> {
                    if (storageSub == 0) {
                        if (units !is LoadState.Ok) units = LoadState.Loading
                        units = runCatching { storageUnits(farmId) }.toLoadState()
                    } else {
                        if (lots !is LoadState.Ok) lots = LoadState.Loading
                        lots = runCatching { storageLots(farmId) }.toLoadState()
                    }
                }
            }
        }
    }
    LaunchedEffect(tab, storageSub, FarmFilter.farmId, LocaleStore.locale) { reload() }
    val canDispatch = canManageFarmOps(meDto?.role)
    val planItems = (plans as? LoadState.Ok)?.items.orEmpty()
    val lotItems = (lots as? LoadState.Ok)?.items.orEmpty()
    val yieldItems = (yieldState as? LoadState.Ok)?.items.orEmpty()
    val selectedPlan = planItems.byId(selectedId) { it.id }
    val selectedLot = lotItems.byId(selectedId) { it.id }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(S.t("harvest.detail.title"), onBack)
        SectionTabs(
            labels = listOf(S.t("tab.plans"), S.t("tab.yield"), S.t("tab.logistics"), S.t("tab.storage")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        msg?.let { Text(it) }
        when (tabs[tab]) {
            HarvestTab.PLANS -> HarvestList(plans) { p ->
                EntityCard(
                    headline = DomainLabels.label(p.crop),
                    supporting = listOfNotNull(p.expectedTHa?.let { "${it}t/ha" }, p.plannedStart).joinToString(" · "),
                    status = DomainLabels.label(p.status),
                    onClick = { onSelect(p.id) },
                )
            }
            HarvestTab.YIELD -> HarvestList(yieldState) { y ->
                    EntityCard(
                    headline = y.yieldTHa?.let { "${it}t/ha" } ?: y.id,
                    supporting = listOfNotNull(y.recordedAt, y.moisturePct?.let { "${S.t("harvest.moisture")} $it%" })
                        .joinToString(" · "),
                )
            }
            HarvestTab.LOGISTICS -> HarvestList(loads) { load ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EntityCard(
                        headline = load.truckPlate ?: load.id,
                        supporting = listOfNotNull(load.destination, load.tons?.let { "${it}t" }).joinToString(" · "),
                        status = DomainLabels.label(load.status),
                    )
                    val queued = load.status?.equals("QUEUED", ignoreCase = true) == true
                    if (queued && canDispatch) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { dispatchLoad(load.id) }
                                    .onSuccess { msg = S.t("harvest.dispatchOk") }
                                    .onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("harvest.dispatch")) }
                    }
                    if (queued && meFailed) {
                        Text(
                            S.t("auth.meLoadError"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (queued && !meFailed && meDto != null && !canDispatch) {
                        Text(
                            S.t("harvest.dispatchNoPermission"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HarvestTab.STORAGE -> {
                SectionTabs(
                    labels = listOf(S.t("tab.units"), S.t("tab.lots")),
                    selectedIndex = storageSub,
                    onSelect = { storageSub = it },
                )
                if (storageSub == 0) {
                    HarvestList(units) { u ->
                        EntityCard(
                            headline = u.name ?: u.id,
                            supporting = listOfNotNull(
                                DomainLabels.label(u.type),
                                u.usedT?.let { "${S.t("charts.used")} $it" },
                                u.capacityT?.let { "${S.t("charts.capacity")} $it" },
                            ).joinToString(" · "),
                        )
                    }
                } else {
                    HarvestList(lots) { lot ->
                        EntityCard(
                            headline = DomainLabels.label(lot.crop),
                            supporting = listOfNotNull(lot.tons?.let { "${it}t" }, lot.quality).joinToString(" · "),
                            onClick = { onSelect(lot.id) },
                        )
                    }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
    if (!selectedId.isNullOrBlank()) {
        when {
            selectedPlan != null -> DetailSheet(
                title = DomainLabels.label(selectedPlan.crop),
                found = true,
                onDismiss = onClearSelected,
            ) { HarvestPlanInspector(selectedPlan, yieldItems) }
            selectedLot != null -> DetailSheet(
                title = DomainLabels.label(selectedLot.crop),
                found = true,
                onDismiss = onClearSelected,
            ) { StorageLotInspector(selectedLot) }
            else -> DetailSheet(
                title = S.t("inspector.notFound"),
                found = false,
                onDismiss = onClearSelected,
            ) {}
        }
    }
}

@Composable
private fun <T> HarvestList(state: LoadState<T>, content: @Composable (T) -> Unit) {
    when (state) {
        is LoadState.Loading -> Text(S.t("common.loading"))
        is LoadState.Err -> Text("${S.t("common.error")}: ${state.message}")
        is LoadState.Ok -> {
            if (state.items.isEmpty()) Text(S.t("common.empty"))
            else state.items.forEach { content(it) }
        }
    }
}
