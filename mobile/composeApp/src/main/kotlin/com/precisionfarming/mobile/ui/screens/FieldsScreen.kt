package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.inspectors.FieldInspector

@Composable
fun FieldsScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    EntityInspectScreen(
        title = S.t("fields.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        load = { fields(FarmFilter.farmId) },
        idOf = { it.id },
        headline = { it.name ?: it.id },
        supporting = {
            listOfNotNull(it.crop, it.areaHa?.let { ha -> "$ha ha" }, it.variety).joinToString(" · ")
        },
        inspector = { FieldInspector(it) },
    )
}
