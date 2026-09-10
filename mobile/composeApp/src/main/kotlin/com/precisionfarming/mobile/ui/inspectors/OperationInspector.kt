package com.precisionfarming.mobile.ui.inspectors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.InventoryItemDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.canComplete
import com.precisionfarming.mobile.data.canPause
import com.precisionfarming.mobile.data.canStart
import com.precisionfarming.mobile.data.completeOp
import com.precisionfarming.mobile.data.formatWhen
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.pauseOp
import com.precisionfarming.mobile.data.startOp
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.InspectorKpiItem
import com.precisionfarming.mobile.ui.components.InspectorKpis
import kotlinx.coroutines.launch

@Composable
fun OperationInspector(
    operation: OperationDto,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var machines by remember { mutableStateOf<List<MachineDto>>(emptyList()) }
    var items by remember { mutableStateOf<List<InventoryItemDto>>(emptyList()) }
    var msg by remember { mutableStateOf<String?>(null) }
    val defaultReason = S.t("ops.pauseReasonDefault")
    var pauseReason by remember(LocaleStore.locale) { mutableStateOf(defaultReason) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(FarmFilter.farmId) {
        machines = runCatching { machines(FarmFilter.farmId) }.getOrDefault(emptyList())
        items = runCatching { inventory(FarmFilter.farmId) }.getOrDefault(emptyList())
    }
    val machine = machines.firstOrNull { it.id == operation.machineId }
    val item = items.firstOrNull { it.id == operation.itemId }
    val inputLabel =
        if (item != null && operation.itemQuantity != null) {
            "${DomainLabels.label(item.name)} · ${operation.itemQuantity} ${item.unit.orEmpty()}"
        } else {
            "—"
        }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(DomainLabels.label(operation.status))
        InspectorKpis(
            listOf(
                InspectorKpiItem(S.t("operations.kpi.planned"), formatWhen(operation.plannedStart)),
                InspectorKpiItem(S.t("operations.kpi.actual"), formatWhen(operation.actualStart)),
                InspectorKpiItem(
                    S.t("operations.kpi.machine"),
                    machine?.name?.let { DomainLabels.label(it) } ?: "—",
                ),
                InspectorKpiItem(S.t("operations.kpi.inputs"), inputLabel),
            ),
        )
        if (!operation.pauseReason.isNullOrBlank()) {
            Text(S.t("operations.pauseMeta", "reason" to DomainLabels.label(operation.pauseReason)))
        }
        if (operation.canPause()) {
            OutlinedTextField(
                pauseReason,
                { pauseReason = it },
                label = { Text(S.t("ops.pauseReason")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        msg?.let { Text(it) }
        if (operation.canStart()) {
            Button(
                onClick = {
                    scope.launch {
                        runCatching { startOp(operation.id) }
                            .onSuccess { onChanged() }
                            .onFailure { msg = it.message }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
            ) { Text(S.t("ops.start")) }
        }
        if (operation.canPause()) {
            Button(
                onClick = {
                    scope.launch {
                        val reason = pauseReason.trim()
                        if (reason.isEmpty()) {
                            msg = S.t("ops.pauseReason")
                            return@launch
                        }
                        runCatching { pauseOp(operation.id, reason) }
                            .onSuccess { onChanged() }
                            .onFailure { msg = it.message }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
            ) { Text(S.t("ops.pause")) }
        }
        if (operation.canComplete()) {
            Button(
                onClick = {
                    scope.launch {
                        runCatching { completeOp(operation.id) }
                            .onSuccess { onChanged() }
                            .onFailure { msg = it.message }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
            ) { Text(S.t("ops.complete")) }
        }
    }
}
