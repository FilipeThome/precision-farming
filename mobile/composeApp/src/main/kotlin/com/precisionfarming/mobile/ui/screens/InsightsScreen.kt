package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.insights
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityInspectScreen
import com.precisionfarming.mobile.ui.inspectors.InsightInspector

@Composable
fun InsightsScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
) {
    EntityInspectScreen(
        title = S.t("insights.title"),
        onBack = onBack,
        selectedId = selectedId,
        onSelect = onSelect,
        onClearSelected = onClearSelected,
        load = { insights(FarmFilter.farmId) },
        idOf = { it.id },
        headline = { DomainLabels.label(it.type) },
        supporting = { "${it.model} · ${it.score}" },
        inspector = { InsightInspector(it) },
    )
}
