package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.theme.AgOsColors

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

/** Segmented PT / EN pill (login mockup). [onDark] flips the palette for the green header. */
@Composable
fun LocalePill(modifier: Modifier = Modifier, onDark: Boolean = false) {
    val current = LocaleStore.locale
    val shape = RoundedCornerShape(999.dp)
    val border = if (onDark) AgOsColors.heroOutline else AgOsColors.n200
    Row(
        modifier
            .border(1.dp, border, shape)
            .padding(3.dp),
    ) {
        PillOption("PT", S.t("login.flag.br"), current == AppLocale.PT_BR, onDark) { LocaleStore.setLocale(AppLocale.PT_BR) }
        PillOption("EN", S.t("login.flag.us"), current == AppLocale.EN_US, onDark) { LocaleStore.setLocale(AppLocale.EN_US) }
    }
}

@Composable
private fun PillOption(label: String, description: String, selected: Boolean, onDark: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    val bg = when {
        selected && onDark -> AgOsColors.heroChip
        selected -> AgOsColors.g100
        else -> Color.Transparent
    }
    val fg = when {
        onDark && selected -> AgOsColors.n0
        onDark -> AgOsColors.heroMuted
        selected -> AgOsColors.g800
        else -> AgOsColors.n600
    }
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(bg, shape)
            .semantics {
                contentDescription = description
                role = Role.RadioButton
                this.selected = selected
            },
        shape = shape,
        colors = ButtonDefaults.textButtonColors(contentColor = fg),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
