package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.data.formatNumber
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.DetailSheet
import com.precisionfarming.mobile.ui.components.EntityCard
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.ScreenHeader
import com.precisionfarming.mobile.ui.components.operationIcon
import com.precisionfarming.mobile.ui.components.toLoadState
import com.precisionfarming.mobile.ui.components.toneForOperationStatus
import com.precisionfarming.mobile.ui.components.toneForStatus
import com.precisionfarming.mobile.ui.inspectors.OperationInspector
import kotlinx.coroutines.launch

@Composable
fun OpsScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onOpenRun: (String) -> Unit = {},
) {
    var state by remember { mutableStateOf<LoadState<OperationDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching { operations(FarmFilter.farmId.value) }.toLoadState()
        }
    }
    LaunchedEffect(LocalFarmId.current, LocalAppLocale.current) { reload() }
    val items = (state as? LoadState.Ok)?.items.orEmpty()
    val selected = items.byId(selectedId) { it.id }
    LazyColumn(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader(S.t("ops.title")) }
        when (val s = state) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("common.error")}: ${s.message}") }
            is LoadState.Ok -> {
                if (s.items.isEmpty()) item { Text(S.t("common.empty")) }
                items(s.items, key = { it.id }) { op ->
                    EntityCard(
                        headline = DomainLabels.label(op.type),
                        supporting = op.areaHa?.let { "${formatNumber(it)} ha" }.orEmpty(),
                        status = DomainLabels.label(op.status),
                        statusTone = toneForStatus(op.status),
                        icon = operationIcon(op.type),
                        iconTone = toneForOperationStatus(op.status),
                        onClick = { onSelect(op.id) },
                    )
                }
            }
        }
        item { TextButton(onClick = { reload() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S.t("common.refresh")) } }
    }
    if (!selectedId.isNullOrBlank()) {
        val row = selected
        DetailSheet(
            title = row?.let { DomainLabels.label(it.type) } ?: S.t("inspector.notFound"),
            subtitle = row?.let { DomainLabels.label(it.status) },
            found = row != null,
            onDismiss = onClearSelected,
        ) {
            if (row != null) {
                OperationInspector(
                    operation = row,
                    onChanged = { reload() },
                    onOpenRun = { id ->
                        onClearSelected()
                        onOpenRun(id)
                    },
                )
            }
        }
    }
}
