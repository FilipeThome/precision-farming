package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.precisionfarming.mobile.data.StorageLotDto
import com.precisionfarming.mobile.data.StorageUnitDto
import com.precisionfarming.mobile.data.YieldRecordDto
import com.precisionfarming.mobile.data.dispatchLoad
import com.precisionfarming.mobile.data.harvestPlans
import com.precisionfarming.mobile.data.harvestYield
import com.precisionfarming.mobile.data.logisticsLoads
import com.precisionfarming.mobile.data.storageLots
import com.precisionfarming.mobile.data.storageUnits
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

private enum class HarvestTab { PLANS, YIELD, LOGISTICS, STORAGE }

@Composable
fun HarvestScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var storageSub by remember { mutableIntStateOf(0) }
    var plans by remember { mutableStateOf<LoadState<HarvestPlanDto>>(LoadState.Loading) }
    var yieldState by remember { mutableStateOf<LoadState<YieldRecordDto>>(LoadState.Loading) }
    var loads by remember { mutableStateOf<LoadState<LogisticsLoadDto>>(LoadState.Loading) }
    var units by remember { mutableStateOf<LoadState<StorageUnitDto>>(LoadState.Loading) }
    var lots by remember { mutableStateOf<LoadState<StorageLotDto>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val tabs = HarvestTab.entries
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId
            when (tabs[tab]) {
                HarvestTab.PLANS -> {
                    plans = LoadState.Loading
                    plans = runCatching { harvestPlans(farmId) }.toLoadState()
                }
                HarvestTab.YIELD -> {
                    yieldState = LoadState.Loading
                    yieldState = runCatching { harvestYield(farmId) }.toLoadState()
                }
                HarvestTab.LOGISTICS -> {
                    loads = LoadState.Loading
                    loads = runCatching { logisticsLoads(farmId) }.toLoadState()
                }
                HarvestTab.STORAGE -> {
                    if (storageSub == 0) {
                        units = LoadState.Loading
                        units = runCatching { storageUnits(farmId) }.toLoadState()
                    } else {
                        lots = LoadState.Loading
                        lots = runCatching { storageLots(farmId) }.toLoadState()
                    }
                }
            }
        }
    }
    LaunchedEffect(tab, storageSub, FarmFilter.farmId, LocaleStore.locale) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text(S.t("common.back")) }
        Text(S.t("harvest.title"))
        SectionTabs(
            labels = listOf(S.t("tab.plans"), S.t("tab.yield"), S.t("tab.logistics"), S.t("tab.storage")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        msg?.let { Text(it) }
        when (tabs[tab]) {
            HarvestTab.PLANS -> LoadedList(plans) { p ->
                listOfNotNull(p.crop, p.status, p.expectedTHa?.let { "${it}t/ha" }, p.plannedStart)
                    .joinToString(" · ")
                    .ifBlank { p.id }
            }
            HarvestTab.YIELD -> LoadedList(yieldState) { y ->
                listOfNotNull(
                    y.recordedAt,
                    y.yieldTHa?.let { "${it}t/ha" },
                    y.moisturePct?.let { "${S.t("harvest.moisture")} $it%" },
                )
                    .joinToString(" · ")
                    .ifBlank { y.id }
            }
            HarvestTab.LOGISTICS -> LoadedList(
                loads,
                format = { load ->
                    listOfNotNull(load.truckPlate, load.destination, load.tons?.let { "${it}t" }, load.status)
                        .joinToString(" · ")
                        .ifBlank { load.id }
                },
                itemContent = { load ->
                    val queued = load.status?.equals("QUEUED", ignoreCase = true) == true
                    if (queued) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { dispatchLoad(load.id) }.onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("harvest.dispatch")) }
                    }
                },
            )
            HarvestTab.STORAGE -> {
                SectionTabs(
                    labels = listOf(S.t("tab.units"), S.t("tab.lots")),
                    selectedIndex = storageSub,
                    onSelect = { storageSub = it },
                )
                if (storageSub == 0) {
                    LoadedList(units) { u ->
                        listOfNotNull(u.name, u.type, u.usedT?.let { "usado $it" }, u.capacityT?.let { "cap $it" })
                            .joinToString(" · ")
                            .ifBlank { u.id }
                    }
                } else {
                    LoadedList(lots) { lot ->
                        listOfNotNull(lot.crop, lot.tons?.let { "${it}t" }, lot.quality, lot.receivedAt)
                            .joinToString(" · ")
                            .ifBlank { lot.id }
                    }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
