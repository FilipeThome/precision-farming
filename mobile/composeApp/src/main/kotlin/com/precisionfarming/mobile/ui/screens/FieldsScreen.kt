package com.precisionfarming.mobile.ui.screens

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.FieldUpsert
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.canWriteMasterData
import com.precisionfarming.mobile.data.centroidOfGeometry
import com.precisionfarming.mobile.data.createField
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.geometryForFieldSave
import com.precisionfarming.mobile.data.patchField
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.components.FormField
import com.precisionfarming.mobile.ui.inspectors.FieldInspector
import kotlinx.coroutines.launch

@Composable
fun FieldsScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var nonce by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<FieldDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var origin by remember { mutableStateOf(mapOf<String, String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canWrite = canWriteMasterData(Session.role)
    EntityInspectScreen(
        title = S.t("fields.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        extraKeys = nonce,
        load = { fields(FarmFilter.farmId.value) },
        idOf = { it.id },
        headline = { it.name ?: it.id },
        supporting = {
            listOfNotNull(it.crop, it.areaHa?.let { ha -> "$ha ha" }, it.variety).joinToString(" · ")
        },
        inspector = { FieldInspector(it) },
        listAction = {
            if (canWrite) {
                TextButton(onClick = {
                    onClearSelected()
                    editing = null
                    creating = true
                    values = mapOf(
                        "name" to "",
                        "crop" to "SOY",
                        "variety" to "",
                        "lat" to "-22.9",
                        "lon" to "-49.9",
                        "areaHa" to "",
                    )
                    origin = values
                    error = null
                }) { Text(S.t("form.new")) }
            }
        },
        sheetAction = { field ->
            if (canWrite) {
                TextButton(onClick = {
                    onClearSelected()
                    creating = false
                    editing = field
                    val c = centroidOfGeometry(field.geometry)
                    values = mapOf(
                        "name" to (field.name ?: ""),
                        "crop" to (field.crop ?: "SOY"),
                        "variety" to (field.variety ?: ""),
                        "lat" to (c?.first?.toString() ?: ""),
                        "lon" to (c?.second?.toString() ?: ""),
                        "areaHa" to (field.areaHa?.toString() ?: ""),
                    )
                    origin = values
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
                FormField("variety", S.t("form.field.variety"), required = false),
                FormField("lat", S.t("form.field.lat")),
                FormField("lon", S.t("form.field.lon")),
                FormField("areaHa", S.t("form.field.areaHa")),
            ),
            values = values,
            onChange = { k, v -> values = values + (k to v) },
            pending = pending,
            error = error,
            onDismiss = { creating = false; editing = null },
            onSave = {
                val farmId = editing?.farmId ?: FarmFilter.farmId.value
                if (farmId.isNullOrBlank()) {
                    error = S.t("form.needFarm")
                    return@EntityFormSheet
                }
                scope.launch {
                    pending = true
                    val body = FieldUpsert(
                        farmId = farmId,
                        name = values["name"].orEmpty(),
                        areaHa = values["areaHa"]?.toDoubleOrNull() ?: 0.0,
                        crop = values["crop"].orEmpty(),
                        variety = values["variety"].orEmpty().ifBlank { null },
                        geometry = geometryForFieldSave(
                            editing?.geometry,
                            origin["lat"].orEmpty(),
                            origin["lon"].orEmpty(),
                            origin["areaHa"].orEmpty(),
                            values["lat"].orEmpty(),
                            values["lon"].orEmpty(),
                            values["areaHa"].orEmpty(),
                        ),
                    )
                    runCatching {
                        val id = editing?.id
                        if (id == null) createField(body) else patchField(id, body)
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
