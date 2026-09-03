package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.ApiListScreen

@Composable
fun InventoryScreen(onBack: () -> Unit) {
    ApiListScreen(S.t("inventory.title"), onBack = onBack) {
        inventory(FarmFilter.farmId).map { item ->
            listOfNotNull(
                item.name,
                item.category,
                item.quantity?.let { q -> "${q}${item.unit.orEmpty()}" },
                item.reserved?.let { "res $it" },
            ).joinToString(" · ").ifBlank { item.id }
        }
    }
}
