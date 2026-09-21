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
import com.precisionfarming.mobile.data.IrrigationAssetDto
import com.precisionfarming.mobile.data.IrrigationAssetUpsert
import com.precisionfarming.mobile.data.IrrigationRecommendationDto
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.canWriteMasterData
import com.precisionfarming.mobile.data.createIrrigationAsset
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.irrigationAssets
import com.precisionfarming.mobile.data.irrigationRecommendations
import com.precisionfarming.mobile.data.patchIrrigationAsset
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.FormField
import com.precisionfarming.mobile.ui.components.FormOption
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.ScreenHeader
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun IrrigationScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var assets by remember { mutableStateOf<LoadState<IrrigationAssetDto>>(LoadState.Loading) }
    var recs by remember { mutableStateOf<LoadState<IrrigationRecommendationDto>>(LoadState.Loading) }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<IrrigationAssetDto?>(null) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var fieldChoices by remember { mutableStateOf(listOf<FormOption>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canWrite = canWriteMasterData(Session.role)
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId
            if (tab == 0) {
                assets = LoadState.Loading
                assets = runCatching { irrigationAssets(farmId) }.toLoadState()
            } else {
                recs = LoadState.Loading
                recs = runCatching { irrigationRecommendations(farmId) }.toLoadState()
            }
        }
    }
    LaunchedEffect(tab, FarmFilter.farmId, LocaleStore.locale) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(S.t("irrigation.title"), onBack, actions = {
            if (tab == 0 && canWrite) {
                TextButton(onClick = {
                    editing = null
                    creating = true
                    values = mapOf("name" to "", "type" to "PIVOT", "status" to "IDLE", "capacityMmH" to "", "fieldId" to "")
                    error = null
                    scope.launch {
                        fieldChoices = runCatching { fields(FarmFilter.farmId) }.getOrDefault(emptyList())
                            .map { FormOption(it.id, it.name ?: it.id) }
                    }
                }) { Text(S.t("form.new")) }
            }
        })
        SectionTabs(
            labels = listOf(S.t("irrigation.assets"), S.t("irrigation.recs")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        if (tab == 0) {
            LoadedList(
                assets,
                itemContent = { a ->
                    if (canWrite) {
                        TextButton(onClick = {
                            creating = false
                            editing = a
                            values = mapOf(
                                "name" to (a.name ?: ""),
                                "type" to (a.type ?: "PIVOT"),
                                "status" to (a.status ?: "IDLE"),
                                "capacityMmH" to (a.capacityMmH?.toString() ?: ""),
                                "fieldId" to (a.fieldId ?: ""),
                            )
                            error = null
                            scope.launch {
                                fieldChoices = runCatching { fields(a.farmId ?: FarmFilter.farmId) }.getOrDefault(emptyList())
                                    .map { FormOption(it.id, it.name ?: it.id) }
                            }
                        }) { Text(S.t("form.edit")) }
                    }
                },
            ) { a ->
                buildString {
                    append(listOfNotNull(a.name, a.type, a.status).joinToString(" · ").ifBlank { a.id })
                    a.capacityMmH?.let { append(" · ${it}mm/h") }
                }
            }
        } else {
            LoadedList(recs) { r ->
                buildString {
                    append(r.id.take(8))
                    r.recommendedMm?.let { append(" · ${it}mm") }
                    r.status?.let { append(" · $it") }
                    r.reason?.let { append(" · $it") }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
    if (creating || editing != null) {
        EntityFormSheet(
            title = if (creating) S.t("form.new") else S.t("form.edit"),
            fields = listOf(
                FormField("name", S.t("form.field.name")),
                FormField("type", S.t("form.field.type")),
                FormField("status", S.t("form.field.status")),
                FormField("capacityMmH", S.t("form.field.capacityMmH"), required = false),
                FormField(
                    "fieldId",
                    S.t("form.field.field"),
                    required = false,
                    options = listOf(FormOption("", S.t("form.none"))) + fieldChoices,
                ),
            ),
            values = values,
            onChange = { k, v -> values = values + (k to v) },
            pending = pending,
            error = error,
            onDismiss = { creating = false; editing = null },
            onSave = {
                val farmId = editing?.farmId ?: FarmFilter.farmId
                if (farmId.isNullOrBlank()) {
                    error = S.t("form.needFarm")
                    return@EntityFormSheet
                }
                scope.launch {
                    pending = true
                    val body = IrrigationAssetUpsert(
                        farmId = farmId,
                        fieldId = values["fieldId"].orEmpty().ifBlank { null },
                        name = values["name"].orEmpty(),
                        type = values["type"].orEmpty(),
                        status = values["status"].orEmpty(),
                        capacityMmH = values["capacityMmH"]?.toDoubleOrNull(),
                    )
                    runCatching {
                        val id = editing?.id
                        if (id == null) createIrrigationAsset(body) else patchIrrigationAsset(id, body)
                    }.onSuccess {
                        creating = false
                        editing = null
                        reload()
                    }.onFailure { error = it.message }
                    pending = false
                }
            },
        )
    }
}
