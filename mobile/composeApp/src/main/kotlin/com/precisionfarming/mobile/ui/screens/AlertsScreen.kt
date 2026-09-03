package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.ackAlert
import com.precisionfarming.mobile.data.alerts
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun AlertsScreen() {
    var state by remember { mutableStateOf<LoadState<AlertDto>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { alerts(FarmFilter.farmId) }.toLoadState()
        }
    }
    LaunchedEffect(FarmFilter.farmId, LocaleStore.locale) { reload() }
    LazyColumn(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text(S.t("alerts.title")) }
        msg?.let { message -> item { Text(message) } }
        when (val s = state) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("common.error")}: ${s.message}") }
            is LoadState.Ok -> {
                if (s.items.isEmpty()) item { Text(S.t("common.empty")) }
                items(s.items, key = { it.id }) { alert ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${alert.title} · ${alert.severity} · ${alert.status}")
                        alert.message?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        if (!alert.status.equals("ACKED", ignoreCase = true)) {
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { ackAlert(alert.id) }.onFailure { msg = it.message }
                                    reload()
                                }
                            }) { Text(S.t("alerts.ack")) }
                        }
                    }
                }
            }
        }
        item { TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) } }
    }
}
