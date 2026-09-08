package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.inspectors.InventoryInspector

@Composable
fun InventoryScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    EntityInspectScreen(
        title = S.t("inventory.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        load = { inventory(FarmFilter.farmId) },
        idOf = { it.id },
        headline = { it.name ?: it.id },
        supporting = {
            listOfNotNull(it.category, it.quantity?.let { q -> "$q ${it.unit.orEmpty()}" }).joinToString(" · ")
        },
        inspector = { InventoryInspector(it) },
    )
}
