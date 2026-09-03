package com.precisionfarming.mobile.ui.screens

import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.ApiListScreen

@Composable
fun FieldsScreen(onBack: () -> Unit) {
    ApiListScreen(S.t("fields.title"), onBack = onBack) {
        fields(FarmFilter.farmId).map { field ->
            listOfNotNull(field.name, field.crop, field.areaHa?.let { "${it}ha" }, field.variety)
                .joinToString(" · ")
                .ifBlank { field.id }
        }
    }
}
