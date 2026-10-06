package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.alerts
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.DetailSheet
import com.precisionfarming.mobile.ui.components.EntityCard
import com.precisionfarming.mobile.ui.components.FreshnessChip
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.ScreenHeader
import com.precisionfarming.mobile.ui.components.entityIcon
import com.precisionfarming.mobile.ui.components.toLoadState
import com.precisionfarming.mobile.ui.components.toneForStatus
import com.precisionfarming.mobile.ui.inspectors.AlertInspector
import kotlinx.coroutines.launch

@Composable
fun AlertsScreen(
    selectedId: String?,
    severity: String?,
    onSelect: (String) -> Unit,
    onSeverity: (String?) -> Unit,
    onClearSelected: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var state by remember { mutableStateOf<LoadState<AlertDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching { alerts(FarmFilter.farmId.value) }.toLoadState()
        }
    }
    LaunchedEffect(LocalFarmId.current, LocalAppLocale.current) { reload() }
    val all = (state as? LoadState.Ok)?.items.orEmpty()
    val filtered = if (severity.isNullOrBlank()) all else all.filter { it.severity.equals(severity, ignoreCase = true) }
    val selected = all.byId(selectedId) { it.id }
    val filters = listOf(null, "CRITICAL", "WARNING", "INFO")
    LazyColumn(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader(S.t("alerts.title")) }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filters.forEach { value ->
                    val label = if (value == null) S.t("alerts.filter.all") else S.t("alerts.filter.$value")
                    FilterChip(
                        selected = (value ?: "") == (severity ?: ""),
                        onClick = { onSeverity(value) },
                        label = { Text(label) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        when (val s = state) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("common.error")}: ${s.message}") }
            is LoadState.Ok -> {
                if (filtered.isEmpty()) item { Text(S.t("common.empty")) }
                items(filtered, key = { it.id }) { alert ->
                    EntityCard(
                        headline = DomainLabels.label(alert.title),
                        supporting = alert.message.orEmpty(),
                        status = "${DomainLabels.label(alert.severity)} · ${DomainLabels.label(alert.status)}",
                        statusTone = toneForStatus(alert.severity),
                        icon = entityIcon("ALERT"),
                        iconTone = toneForStatus(alert.severity),
                        onClick = { onSelect(alert.id) },
                        trailing = { FreshnessChip(alert.createdAt) },
                    )
                }
            }
        }
        item { TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) } }
    }
    if (!selectedId.isNullOrBlank()) {
        val row = selected
        DetailSheet(
            title = row?.let { DomainLabels.label(it.title) } ?: S.t("inspector.notFound"),
            found = row != null,
            onDismiss = onClearSelected,
        ) {
            if (row != null) AlertInspector(alert = row, onChanged = { reload() }, onOpen = onOpen)
        }
    }
}
