package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.integrations
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.ApiListScreen

@Composable
fun IntegrationsScreen(onBack: () -> Unit) {
    ApiListScreen(S.t("integrations.title"), onBack = onBack) {
        integrations().map { item ->
            listOfNotNull(item.name, item.type, item.mode, item.capabilities.takeIf { it.isNotEmpty() }?.joinToString(","))
                .joinToString(" · ")
        }
    }
}
