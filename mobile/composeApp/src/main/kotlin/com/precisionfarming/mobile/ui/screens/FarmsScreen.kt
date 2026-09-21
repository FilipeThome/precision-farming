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
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FarmUpsert
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.canCreateFarm
import com.precisionfarming.mobile.data.canWriteMasterData
import com.precisionfarming.mobile.data.createFarm
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.data.patchFarm
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.components.FormField
import com.precisionfarming.mobile.ui.inspectors.FarmInspector
import kotlinx.coroutines.launch

@Composable
fun FarmsScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var nonce by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<FarmDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val formOpen = creating || editing != null
    EntityInspectScreen(
        title = S.t("farms.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        extraKeys = nonce,
        load = { farms() },
        idOf = { it.id },
        headline = { it.name },
        supporting = { listOfNotNull(it.location, it.areaHa?.let { ha -> "$ha ha" }).joinToString(" · ") },
        inspector = { FarmInspector(it) },
        listAction = {
            if (canCreateFarm(Session.role)) {
                TextButton(onClick = {
                    onClearSelected()
                    editing = null
                    creating = true
                    values = mapOf("name" to "", "location" to "", "areaHa" to "", "timezone" to "America/Sao_Paulo")
                    error = null
                }) { Text(S.t("form.new")) }
            }
        },
        sheetAction = { farm ->
            if (canWriteMasterData(Session.role)) {
                TextButton(onClick = {
                    onClearSelected()
                    creating = false
                    editing = farm
                    values = mapOf(
                        "name" to farm.name,
                        "location" to farm.location,
                        "areaHa" to (farm.areaHa?.toString() ?: ""),
                        "timezone" to (farm.timezone ?: "America/Sao_Paulo"),
                    )
                    error = null
                }) { Text(S.t("form.edit")) }
            }
        },
    )
    if (formOpen) {
        EntityFormSheet(
            title = if (creating) S.t("form.new") else S.t("form.edit"),
            fields = listOf(
                FormField("name", S.t("form.field.name")),
                FormField("location", S.t("form.field.location")),
                FormField("areaHa", S.t("form.field.areaHa")),
                FormField("timezone", S.t("form.field.timezone")),
            ),
            values = values,
            onChange = { k, v -> values = values + (k to v) },
            pending = pending,
            error = error,
            onDismiss = { creating = false; editing = null },
            onSave = {
                scope.launch {
                    pending = true
                    error = null
                    val body = FarmUpsert(
                        values["name"].orEmpty(),
                        values["location"].orEmpty(),
                        values["areaHa"]?.toDoubleOrNull() ?: 0.0,
                        values["timezone"].orEmpty().ifBlank { "America/Sao_Paulo" },
                    )
                    runCatching {
                        val id = editing?.id
                        if (id == null) createFarm(body) else patchFarm(id, body)
                    }.onSuccess {
                        creating = false
                        editing = null
                        nonce += 1
                    }.onFailure {
                        error = if (it.message == "SESSION_REFRESH_FAILED") S.t("form.sessionRefreshFailed") else it.message
                    }
                    pending = false
                }
            },
        )
    }
}
