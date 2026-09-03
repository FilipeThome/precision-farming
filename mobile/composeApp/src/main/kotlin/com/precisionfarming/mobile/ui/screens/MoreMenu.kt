package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.i18n.S

@Composable
fun MoreMenu(onOpen: (String) -> Unit) {
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(S.t("more.title"), style = MaterialTheme.typography.titleLarge)
        Group(S.t("more.group.land")) {
            Item("mais/farms", "more.farms", onOpen)
            Item("mais/fields", "more.fields", onOpen)
            Item("mais/seasons", "more.seasons", onOpen)
        }
        Group(S.t("more.group.fleet")) {
            Item("mais/machines", "more.machines", onOpen)
            Item("mais/maintenance", "more.maintenance", onOpen)
        }
        Group(S.t("more.group.agronomy")) {
            Item("mais/agronomy", "more.agronomy", onOpen)
        }
        Group(S.t("more.group.water")) {
            Item("mais/weather", "more.weather", onOpen)
            Item("mais/irrigation", "more.irrigation", onOpen)
        }
        Group(S.t("more.group.harvest")) {
            Item("mais/harvest", "more.harvest", onOpen)
        }
        Group(S.t("more.group.inventory")) {
            Item("mais/inventory", "more.inventory", onOpen)
        }
        Group(S.t("more.group.finance")) {
            Item("mais/finance", "more.finance", onOpen)
            Item("mais/market", "more.market", onOpen)
        }
        Group(S.t("more.group.compliance")) {
            Item("mais/compliance", "more.compliance", onOpen)
        }
        Group(S.t("more.group.insights")) {
            Item("mais/insights", "more.insights", onOpen)
            Item("mais/reports", "more.reports", onOpen)
            Item("mais/integrations", "more.integrations", onOpen)
            Item("mais/settings", "more.settings", onOpen)
        }
        Group(S.t("more.group.sync")) {
            Item("mais/sync", "more.sync", onOpen)
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    content()
}

@Composable
private fun Item(route: String, labelKey: String, onOpen: (String) -> Unit) {
    TextButton(onClick = { onOpen(route) }) { Text(S.t(labelKey)) }
}
