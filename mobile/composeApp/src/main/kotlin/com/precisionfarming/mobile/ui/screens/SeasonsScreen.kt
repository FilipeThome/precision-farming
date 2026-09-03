package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.seasons
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.ApiListScreen

@Composable
fun SeasonsScreen(onBack: () -> Unit) {
    ApiListScreen(S.t("seasons.title"), onBack = onBack) {
        seasons(FarmFilter.farmId).map { season ->
            listOfNotNull(season.name, season.crop, season.status, season.year?.toString())
                .joinToString(" · ")
                .ifBlank { season.id }
        }
    }
}
