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
import com.precisionfarming.mobile.data.InventoryCreate
import com.precisionfarming.mobile.data.InventoryItemDto
import com.precisionfarming.mobile.data.InventoryPatch
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.canWriteMasterData
import com.precisionfarming.mobile.data.createInventoryItem
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.data.patchInventoryItem
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.components.FormField
import com.precisionfarming.mobile.ui.inspectors.InventoryInspector
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var nonce by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<InventoryItemDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canWrite = canWriteMasterData(Session.role)
    EntityInspectScreen(
        title = S.t("inventory.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        extraKeys = nonce,
        load = { inventory(FarmFilter.farmId.value) },
        idOf = { it.id },
        headline = { it.name ?: it.id },
        supporting = {
            listOfNotNull(it.category, it.quantity?.let { q -> "$q ${it.unit.orEmpty()}" }).joinToString(" · ")
        },
        inspector = { InventoryInspector(it) },
        listAction = {
            if (canWrite) {
                TextButton(onClick = {
                    onClearSelected()
                    editing = null
                    creating = true
                    values = mapOf("name" to "", "category" to "FERTILIZER", "unit" to "KG", "quantity" to "")
                    error = null
                }) { Text(S.t("form.new")) }
            }
        },
        sheetAction = { item ->
            if (canWrite) {
                TextButton(onClick = {
                    onClearSelected()
                    creating = false
                    editing = item
                    values = mapOf(
                        "name" to (item.name ?: ""),
                        "category" to (item.category ?: "FERTILIZER"),
                        "unit" to (item.unit ?: "KG"),
                    )
                    error = null
                }) { Text(S.t("form.edit")) }
            }
        },
    )
    if (creating || editing != null) {
        EntityFormSheet(
            title = if (creating) S.t("form.new") else S.t("form.edit"),
            fields = buildList {
                add(FormField("name", S.t("form.field.name")))
                add(FormField("category", S.t("form.field.category")))
                add(FormField("unit", S.t("form.field.unit")))
                if (creating) add(FormField("quantity", S.t("form.field.quantity")))
            },
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
                    runCatching {
                        if (creating) {
                            createInventoryItem(
                                InventoryCreate(
                                    farmId,
                                    values["name"].orEmpty(),
                                    values["category"].orEmpty(),
                                    values["unit"].orEmpty(),
                                    values["quantity"]?.toDoubleOrNull() ?: 0.0,
                                ),
                            )
                        } else {
                            patchInventoryItem(
                                editing!!.id,
                                InventoryPatch(
                                    farmId,
                                    values["name"].orEmpty(),
                                    values["category"].orEmpty(),
                                    values["unit"].orEmpty(),
                                ),
                            )
                        }
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
