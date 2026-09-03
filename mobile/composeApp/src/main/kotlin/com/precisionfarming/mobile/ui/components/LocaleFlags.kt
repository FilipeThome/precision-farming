package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S

@Composable
fun LocaleFlagButtons() {
    val current = LocaleStore.locale
    Row {
        TextButton(
            onClick = { LocaleStore.setLocale(AppLocale.PT_BR) },
            enabled = current != AppLocale.PT_BR,
        ) {
            Text("🇧🇷 ${S.t("locale.br")}")
        }
        TextButton(
            onClick = { LocaleStore.setLocale(AppLocale.EN_US) },
            enabled = current != AppLocale.EN_US,
        ) {
            Text("🇺🇸 ${S.t("locale.us")}")
        }
    }
}
