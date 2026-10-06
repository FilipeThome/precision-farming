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
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.MachineUpsert
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.canWriteFleet
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.saveMachineWithPhoto
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.components.FormField
import com.precisionfarming.mobile.ui.inspectors.MachineInspector
import com.precisionfarming.mobile.ui.platform.PickedImage
import com.precisionfarming.mobile.ui.platform.rememberImagePickerLauncher
import kotlinx.coroutines.launch

@Composable
fun MachinesScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var nonce by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<MachineDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var photo by remember { mutableStateOf<PickedImage?>(null) }
    var removePhoto by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pick = rememberImagePickerLauncher(
        onPicked = { photo = it; error = null },
        onError = { error = S.t("form.photoInvalid") },
    )
    val canWrite = canWriteFleet(Session.role)
    EntityInspectScreen(
        title = S.t("machines.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        extraKeys = nonce,
        load = { machines(FarmFilter.farmId.value) },
        idOf = { it.id },
        headline = { DomainLabels.label(it.name) },
        supporting = { DomainLabels.label(it.type) },
        statusOf = { DomainLabels.label(it.status) },
        header = { Text(S.t("machines.selectHint")) },
        inspector = { MachineInspector(it) },
        listAction = {
            if (canWrite) {
                TextButton(onClick = {
                    onClearSelected()
                    editing = null
                    creating = true
                    photo = null
                    removePhoto = false
                    values = mapOf(
                        "name" to "",
                        "type" to "TRACTOR",
                        "manufacturer" to "",
                        "model" to "",
                        "status" to "IDLE",
                    )
                    error = null
                }) { Text(S.t("form.new")) }
            }
        },
        sheetAction = { machine ->
            if (canWrite) {
                TextButton(onClick = {
                    onClearSelected()
                    creating = false
                    editing = machine
                    photo = null
                    removePhoto = false
                    values = mapOf(
                        "name" to machine.name,
                        "type" to machine.type,
                        "manufacturer" to (machine.manufacturer ?: ""),
                        "model" to (machine.model ?: ""),
                        "status" to machine.status,
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
                FormField("type", S.t("form.field.type")),
                FormField("manufacturer", S.t("form.field.manufacturer")),
                FormField("model", S.t("form.field.model")),
                FormField("status", S.t("form.field.status")),
            ),
            values = values,
            onChange = { k, v -> values = values + (k to v) },
            pending = pending,
            error = error,
            extra = {
                TextButton(onClick = pick) { Text(if (photo == null) S.t("form.photo") else S.t("form.photoPicked")) }
                if (editing?.photoFileId != null && photo == null && !removePhoto) {
                    TextButton(onClick = { removePhoto = true }) { Text(S.t("form.photoRemove")) }
                }
            },
            onDismiss = { creating = false; editing = null },
            onSave = {
                val farmId = editing?.farmId ?: FarmFilter.farmId.value
                if (farmId.isNullOrBlank()) {
                    error = S.t("form.needFarm")
                    return@EntityFormSheet
                }
                scope.launch {
                    pending = true
                    val body = MachineUpsert(
                        farmId = farmId,
                        name = values["name"].orEmpty(),
                        type = values["type"].orEmpty(),
                        manufacturer = values["manufacturer"].orEmpty(),
                        model = values["model"].orEmpty(),
                        status = values["status"].orEmpty(),
                        photoFileId = if (removePhoto && photo == null) null else editing?.photoFileId,
                    )
                    runCatching {
                        saveMachineWithPhoto(editing?.id, body, photo?.bytes, photo?.mimeType)
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
