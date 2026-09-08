package com.precisionfarming.mobile.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.inspectors.MachineInspector

@Composable
fun MachinesScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    EntityInspectScreen(
        title = S.t("machines.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        load = { machines(FarmFilter.farmId) },
        idOf = { it.id },
        headline = { DomainLabels.label(it.name) },
        supporting = { DomainLabels.label(it.type) },
        statusOf = { DomainLabels.label(it.status) },
        header = { Text(S.t("machines.selectHint")) },
        inspector = { MachineInspector(it) },
    )
}
