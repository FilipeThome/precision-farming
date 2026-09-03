package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.ApiListScreen

@Composable
fun FarmsScreen(onBack: () -> Unit) {
    ApiListScreen(S.t("farms.title"), onBack = onBack) {
        farms().map { farm ->
            listOfNotNull(farm.name, farm.location, farm.areaHa?.let { "${it}ha" })
                .joinToString(" · ")
        }
    }
}
