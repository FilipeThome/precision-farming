package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = S.t("login.flag.br") },
        ) {
            Text("🇧🇷 ${S.t("locale.br")}")
        }
        TextButton(
            onClick = { LocaleStore.setLocale(AppLocale.EN_US) },
            enabled = current != AppLocale.EN_US,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = S.t("login.flag.us") },
        ) {
            Text("🇺🇸 ${S.t("locale.us")}")
        }
    }
}
