package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.inspectors.FarmInspector

@Composable
fun FarmsScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    EntityInspectScreen(
        title = S.t("farms.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        load = { farms() },
        idOf = { it.id },
        headline = { it.name },
        supporting = { listOfNotNull(it.location, it.areaHa?.let { ha -> "$ha ha" }).joinToString(" · ") },
        inspector = { FarmInspector(it) },
    )
}
