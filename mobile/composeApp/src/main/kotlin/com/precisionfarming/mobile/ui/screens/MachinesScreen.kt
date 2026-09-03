package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.ApiListScreen

@Composable
fun MachinesScreen(onBack: () -> Unit) {
    ApiListScreen(S.t("machines.title"), onBack = onBack) {
        machines(FarmFilter.farmId).map { "${it.name} · ${it.status} · ${it.type}" }
    }
}
