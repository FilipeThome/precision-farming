package com.precisionfarming.mobile.ui.screens

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.SeasonDto
import com.precisionfarming.mobile.data.SeasonUpsert
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.canWriteMasterData
import com.precisionfarming.mobile.data.createSeason
import com.precisionfarming.mobile.data.patchSeason
import com.precisionfarming.mobile.data.seasons
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.components.FormField
import kotlinx.coroutines.launch

@Composable
fun SeasonsScreen(onBack: () -> Unit) {
    var nonce by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<SeasonDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canWrite = canWriteMasterData(Session.role)
    EntityInspectScreen(
        title = S.t("seasons.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = { selectedId = it },
        onClearSelected = { selectedId = null },
        extraKeys = nonce,
        load = { seasons(FarmFilter.farmId) },
        idOf = { it.id },
        headline = { it.name ?: it.id },
        supporting = { listOfNotNull(it.crop, it.status).joinToString(" · ") },
        inspector = { season ->
            Text(listOfNotNull(season.crop, season.startDate, season.endDate).joinToString(" · "))
        },
        listAction = {
            if (canWrite) {
                TextButton(onClick = {
                    selectedId = null
                    editing = null
                    creating = true
                    values = mapOf("name" to "", "crop" to "SOY", "startDate" to "2026-09-01", "endDate" to "", "status" to "PLANNED")
                    error = null
                }) { Text(S.t("form.new")) }
            }
        },
        sheetAction = { season ->
            if (canWrite) {
                TextButton(onClick = {
                    selectedId = null
                    creating = false
                    editing = season
                    values = mapOf(
                        "name" to (season.name ?: ""),
                        "crop" to (season.crop ?: "SOY"),
                        "startDate" to (season.startDate ?: ""),
                        "endDate" to (season.endDate ?: ""),
                        "status" to (season.status ?: "PLANNED"),
                    )
                    error = null
                }) { Text(S.t("form.edit")) }
            }
        },
    )
    if (creating || editing != null) {
        EntityFormSheet(
            title = if (creating) S.t("form.new") else S.t("form.edit"),
            fields = listOf(
                FormField("name", S.t("form.field.name")),
                FormField("crop", S.t("form.field.crop")),
                FormField("startDate", S.t("form.field.startDate")),
                FormField("endDate", S.t("form.field.endDate"), required = false),
                FormField("status", S.t("form.field.status")),
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
                    val body = SeasonUpsert(
                        farmId,
                        values["name"].orEmpty(),
                        values["crop"].orEmpty(),
                        values["startDate"].orEmpty(),
                        values["endDate"].orEmpty().ifBlank { null },
                        values["status"].orEmpty(),
                    )
                    runCatching {
                        val id = editing?.id
                        if (id == null) createSeason(body) else patchSeason(id, body)
                    }.onSuccess {
                        creating = false
                        editing = null
                        nonce += 1
                    }.onFailure { error = it.message }
                    pending = false
                }
            },
        )
    }
}
