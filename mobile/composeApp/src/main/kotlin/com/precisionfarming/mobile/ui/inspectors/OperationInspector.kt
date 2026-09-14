package com.precisionfarming.mobile.ui.inspectors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.InventoryItemDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.canComplete
import com.precisionfarming.mobile.data.canPause
import com.precisionfarming.mobile.data.canStart
import com.precisionfarming.mobile.data.withQueuedStatus
import com.precisionfarming.mobile.data.formatWhen
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.data.offline.OpCommand
import com.precisionfarming.mobile.data.offline.SyncState
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.InspectorKpiItem
import com.precisionfarming.mobile.ui.components.InspectorKpis
import com.precisionfarming.mobile.ui.components.PauseReasonSheet
import com.precisionfarming.mobile.ui.components.StatusTag
import com.precisionfarming.mobile.ui.components.TagTone
import com.precisionfarming.mobile.ui.components.toneForStatus

/** Operation detail sheet. Commands are enqueued through the offline queue (idempotent replay). */
@Composable
fun OperationInspector(
    operation: OperationDto,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenRun: ((String) -> Unit)? = null,
) {
    var machines by remember { mutableStateOf<List<MachineDto>>(emptyList()) }
    var items by remember { mutableStateOf<List<InventoryItemDto>>(emptyList()) }
    var pauseSheet by remember { mutableStateOf(false) }
    LaunchedEffect(FarmFilter.farmId) {
        machines = runCatching { machines(FarmFilter.farmId) }.getOrDefault(emptyList())
        items = runCatching { inventory(FarmFilter.farmId) }.getOrDefault(emptyList())
    }
    val queue by OfflineRuntime.queue.state.collectAsState()
    val shown = operation.withQueuedStatus(queue)
    val open = queue.openFor(operation.id)
    val failed = queue.failedFor(operation.id)
    val syncedForOp = queue.items.count { it.operationId == operation.id && it.state == SyncState.SYNCED }
    LaunchedEffect(syncedForOp) { if (syncedForOp > 0) onChanged() }

    val machine = machines.firstOrNull { it.id == operation.machineId }
    val item = items.firstOrNull { it.id == operation.itemId }
    val inputLabel =
        if (item != null && operation.itemQuantity != null) {
            "${DomainLabels.label(item.name)} · ${operation.itemQuantity} ${item.unit.orEmpty()}"
        } else {
            "—"
        }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusTag(DomainLabels.label(shown.status), tone = toneForStatus(shown.status), large = true)
            if (open != null) {
                StatusTag(
                    S.t("run.pendingSync"),
                    tone = TagTone.Warn,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
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
        failed.forEach { f ->
            Text(
                S.t("run.syncFailed", "error" to (f.lastError ?: S.t("common.error"))),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val busy = open != null
        if (shown.canStart()) {
            Button(
                onClick = { OfflineRuntime.enqueueAndSync(OpCommand.Start(operation.id, shown.status)) },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) { Text(if (shown.status.equals("PAUSED", ignoreCase = true)) S.t("today.continue") else S.t("ops.start")) }
        }
        if (shown.canPause()) {
            OutlinedButton(
                onClick = { pauseSheet = true },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.t("ops.pause")) }
        }
        if (shown.canComplete()) {
            Button(
                onClick = { OfflineRuntime.enqueueAndSync(OpCommand.Complete(operation.id, shown.status)) },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.t("ops.complete")) }
        }
        if (onOpenRun != null) {
            OutlinedButton(
                onClick = { onOpenRun(operation.id) },
                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.t("today.open")) }
        }
    }
    if (pauseSheet) {
        PauseReasonSheet(
            onDismiss = { pauseSheet = false },
            onConfirm = { reason ->
                pauseSheet = false
                OfflineRuntime.enqueueAndSync(OpCommand.Pause(operation.id, reason, shown.status))
            },
        )
    }
}
