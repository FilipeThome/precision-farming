package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.HarvestPlanCreate
import com.precisionfarming.mobile.data.HarvestPlanDto
import com.precisionfarming.mobile.data.LogisticsLoadDto
import com.precisionfarming.mobile.data.MeDto
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.StorageLotDto
import com.precisionfarming.mobile.data.StorageUnitDto
import com.precisionfarming.mobile.data.StorageUnitUpsert
import com.precisionfarming.mobile.data.YieldRecordDto
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.data.canManageFarmOps
import com.precisionfarming.mobile.data.canWriteMasterData
import com.precisionfarming.mobile.data.createHarvestPlan
import com.precisionfarming.mobile.data.createStorageUnit
import com.precisionfarming.mobile.data.dispatchLoad
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.harvestPlans
import com.precisionfarming.mobile.data.harvestYield
import com.precisionfarming.mobile.data.logisticsLoads
import com.precisionfarming.mobile.data.me
import com.precisionfarming.mobile.data.patchStorageUnit
import com.precisionfarming.mobile.data.storageLots
import com.precisionfarming.mobile.data.storageUnits
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.DetailSheet
import com.precisionfarming.mobile.ui.components.EntityCard
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.FormField
import com.precisionfarming.mobile.ui.components.FormOption
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
    var harvestForm by remember { mutableStateOf<String?>(null) }
    var editingUnit by remember { mutableStateOf<StorageUnitDto?>(null) }
    var formValues by remember { mutableStateOf(mapOf<String, String>()) }
    var fieldChoices by remember { mutableStateOf(listOf<FormOption>()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var formPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tabs = HarvestTab.entries
    val canWrite = canWriteMasterData(Session.role)
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId.value
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
    LaunchedEffect(tab, storageSub, LocalFarmId.current, LocalAppLocale.current) { reload() }
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
        ScreenHeader(S.t("harvest.detail.title"), onBack, actions = {
            val showNew = canWrite && (tabs[tab] == HarvestTab.PLANS || (tabs[tab] == HarvestTab.STORAGE && storageSub == 0))
            if (showNew) {
                TextButton(onClick = {
                    formError = null
                    editingUnit = null
                    if (tabs[tab] == HarvestTab.PLANS) {
                        harvestForm = "plan"
                        formValues = mapOf("fieldId" to "", "crop" to "SOY", "expectedTHa" to "")
                        scope.launch {
                            fieldChoices = runCatching { fields(FarmFilter.farmId.value) }.getOrDefault(emptyList())
                                .map { FormOption(it.id, it.name ?: it.id) }
                            val first = fieldChoices.firstOrNull()?.value
                            if (first != null) formValues = formValues + ("fieldId" to first)
                        }
                    } else {
                        harvestForm = "unit"
                        formValues = mapOf("name" to "", "type" to "SILO", "capacityT" to "")
                    }
                }) { Text(S.t("form.new")) }
            }
        })
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            EntityCard(
                                headline = u.name ?: u.id,
                                supporting = listOfNotNull(
                                    DomainLabels.label(u.type),
                                    u.usedT?.let { "${S.t("charts.used")} $it" },
                                    u.capacityT?.let { "${S.t("charts.capacity")} $it" },
                                ).joinToString(" · "),
                            )
                            if (canWrite) {
                                TextButton(onClick = {
                                    editingUnit = u
                                    harvestForm = "unit-edit"
                                    formValues = mapOf(
                                        "name" to (u.name ?: ""),
                                        "type" to (u.type ?: "SILO"),
                                        "capacityT" to (u.capacityT?.toString() ?: ""),
                                    )
                                    formError = null
                                }) { Text(S.t("form.edit")) }
                            }
                        }
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
    if (harvestForm != null) {
        val isPlan = harvestForm == "plan"
        EntityFormSheet(
            title = if (harvestForm == "unit-edit") S.t("form.edit") else S.t("form.new"),
            fields = if (isPlan) {
                listOf(
                    FormField("fieldId", S.t("form.field.field"), options = fieldChoices),
                    FormField("crop", S.t("form.field.crop")),
                    FormField("expectedTHa", S.t("form.field.expectedTHa")),
                )
            } else {
                listOf(
                    FormField("name", S.t("form.field.name")),
                    FormField("type", S.t("form.field.type")),
                    FormField("capacityT", S.t("form.field.capacityT")),
                )
            },
            values = formValues,
            onChange = { k, v -> formValues = formValues + (k to v) },
            pending = formPending,
            error = formError,
            onDismiss = { harvestForm = null; editingUnit = null },
            onSave = {
                val farmId = FarmFilter.farmId.value
                if (farmId.isNullOrBlank()) {
                    formError = S.t("form.needFarm")
                    return@EntityFormSheet
                }
                scope.launch {
                    formPending = true
                    runCatching {
                        when (harvestForm) {
                            "plan" -> createHarvestPlan(
                                HarvestPlanCreate(
                                    farmId,
                                    formValues["fieldId"].orEmpty(),
                                    formValues["crop"].orEmpty(),
                                    formValues["expectedTHa"]?.toDoubleOrNull() ?: 0.0,
                                ),
                            )
                            else -> {
                                val unit = editingUnit
                                val body = StorageUnitUpsert(
                                    farmId,
                                    formValues["name"].orEmpty(),
                                    formValues["type"].orEmpty(),
                                    formValues["capacityT"]?.toDoubleOrNull() ?: 0.0,
                                    unit?.usedT ?: 0.0,
                                )
                                if (unit == null) createStorageUnit(body) else patchStorageUnit(unit.id, body)
                            }
                        }
                    }.onSuccess {
                        harvestForm = null
                        editingUnit = null
                        reload()
                    }.onFailure { formError = it.message }
                    formPending = false
                }
            },
        )
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
