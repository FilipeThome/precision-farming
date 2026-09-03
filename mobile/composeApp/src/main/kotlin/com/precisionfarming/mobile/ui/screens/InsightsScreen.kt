package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.insights
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.ApiListScreen

@Composable
fun InsightsScreen(onBack: () -> Unit) {
    ApiListScreen(S.t("insights.title"), onBack = onBack) {
        insights(FarmFilter.farmId).map { "${it.type} (${it.model}) · ${it.score}" }
    }
}
