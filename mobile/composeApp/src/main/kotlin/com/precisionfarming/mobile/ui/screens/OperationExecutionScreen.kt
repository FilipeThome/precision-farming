package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.InventoryItemDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.TimeFormat
import com.precisionfarming.mobile.data.TodayOps
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.data.canComplete
import com.precisionfarming.mobile.data.canPause
import com.precisionfarming.mobile.data.canStart
import com.precisionfarming.mobile.data.withQueuedStatus
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.formatNumber
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.data.offline.OpCommand
import com.precisionfarming.mobile.data.offline.SyncState
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.AgCard
import com.precisionfarming.mobile.ui.components.KpiCard
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.PauseReasonSheet
import com.precisionfarming.mobile.ui.components.ScreenHeader
import com.precisionfarming.mobile.ui.components.StatusTag
import com.precisionfarming.mobile.ui.components.TagTone
import com.precisionfarming.mobile.ui.components.toneForStatus
import com.precisionfarming.mobile.ui.theme.AgOsColors
import com.precisionfarming.mobile.ui.theme.MonoSmall
import com.precisionfarming.mobile.ui.theme.MonoTimer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private data class RunBundle(
    val operation: OperationDto?,
    val farms: List<FarmDto>,
    val fields: List<FieldDto>,
    val machines: List<MachineDto>,
    val items: List<InventoryItemDto>,
)

/**
 * Work-order execution. Honest to server state: elapsed only from `actualStart` (ticks only while
 * IN_PROGRESS), remaining only from `plannedEnd`, and every action goes through the offline queue.
 */
@Composable
fun OperationExecutionScreen(operationId: String, onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<RunBundle>>(LoadState.Loading) }
    var pauseSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching {
                val farmId = FarmFilter.farmId
                coroutineScope {
                    val opJob = async { operations(farmId) }
                    val farmJob = async { runCatching { farms() }.getOrDefault(emptyList()) }
                    val fieldJob = async { runCatching { fields(farmId) }.getOrDefault(emptyList()) }
                    val machineJob = async { runCatching { machines(farmId) }.getOrDefault(emptyList()) }
                    val itemJob = async { runCatching { inventory(farmId) }.getOrDefault(emptyList()) }
                    RunBundle(
                        operation = opJob.await().byId(operationId) { it.id },
                        farms = farmJob.await(),
                        fields = fieldJob.await(),
                        machines = machineJob.await(),
                        items = itemJob.await(),
                    )
                }
            }.fold(
                onSuccess = { LoadState.Ok(listOf(it)) },
                onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
            )
        }
    }
    LaunchedEffect(operationId, FarmFilter.farmId, LocaleStore.locale) { reload() }
    val queue by OfflineRuntime.queue.state.collectAsState()
    val syncedForOp = queue.items.count { it.operationId == operationId && it.state == SyncState.SYNCED }
    LaunchedEffect(syncedForOp) { if (syncedForOp > 0) reload() }

    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val s = state) {
            is LoadState.Loading -> {
                ScreenHeader(S.t("run.title"), onBack)
                Text(S.t("common.loading"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is LoadState.Err -> {
                ScreenHeader(S.t("run.title"), onBack)
                Text("${S.t("common.error")}: ${s.message}", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { reload() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S.t("common.refresh")) }
            }
            is LoadState.Ok -> {
                val b = s.items.first()
                val op = b.operation
                if (op == null) {
                    ScreenHeader(S.t("run.title"), onBack)
                    Text(S.t("inspector.notFound"), style = MaterialTheme.typography.titleMedium)
                    Text(S.t("inspector.notFoundHint"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val zone = TodayOps.resolveZone(b.farms, FarmFilter.farmId)
                    val field = b.fields.firstOrNull { it.id == op.fieldId }
                    val machine = b.machines.firstOrNull { it.id == op.machineId }
                    val item = b.items.firstOrNull { it.id == op.itemId }
                    val title = listOfNotNull(DomainLabels.label(op.type), field?.name?.let { DomainLabels.label(it) }).joinToString(" · ")
                    val shown = op.withQueuedStatus(queue)
                    val open = queue.openFor(op.id)
                    val failed = queue.failedFor(op.id)

                    ScreenHeader(title, onBack) {
                        StatusTag(DomainLabels.label(shown.status), tone = toneForStatus(shown.status), large = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(op.id.take(8), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (open != null) {
                            StatusTag(
                                S.t("run.pendingSync"),
                                tone = TagTone.Warn,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                    failed.forEach { f ->
                        Text(
                            S.t("run.syncFailed", "error" to (f.lastError ?: S.t("common.error"))),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }

                    TimerCard(shown, zone)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(
                            S.t("run.area"),
                            op.areaHa?.let { "${formatNumber(it)} ha" } ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                        KpiCard(
                            S.t("operations.kpi.machine"),
                            machine?.name?.let { DomainLabels.label(it) } ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (item != null && op.itemQuantity != null) {
                        KpiCard(
                            S.t("run.input"),
                            "${formatNumber(op.itemQuantity)} ${item.unit.orEmpty()}".trim(),
                            hint = DomainLabels.label(item.name),
                        )
                    }
                    if (!op.pauseReason.isNullOrBlank() && shown.status.equals("PAUSED", ignoreCase = true)) {
                        AgCard(Modifier.fillMaxWidth()) {
                            Text(
                                S.t("operations.pauseMeta", "reason" to DomainLabels.label(op.pauseReason)),
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    val busy = open != null
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (shown.canPause()) {
                            OutlinedButton(
                                onClick = { pauseSheet = true },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AgOsColors.warnBg,
                                    contentColor = AgOsColors.warn,
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, AgOsColors.warnBorder),
                            ) {
                                Icon(Icons.Outlined.Pause, contentDescription = null)
                                Text(S.t("ops.pause"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        if (shown.canStart()) {
                            Button(
                                onClick = { OfflineRuntime.enqueueAndSync(OpCommand.Start(op.id, shown.status)) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AgOsColors.t500, contentColor = AgOsColors.n0),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text(
                                    if (shown.status.equals("PAUSED", ignoreCase = true)) S.t("today.continue") else S.t("ops.start"),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                        if (shown.canComplete()) {
                            Button(
                                onClick = { OfflineRuntime.enqueueAndSync(OpCommand.Complete(op.id, shown.status)) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AgOsColors.t600, contentColor = AgOsColors.n0),
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                Text(S.t("ops.complete"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    TextButton(onClick = { reload() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S.t("common.refresh")) }

                    if (pauseSheet) {
                        PauseReasonSheet(
                            onDismiss = { pauseSheet = false },
                            onConfirm = { reason ->
                                pauseSheet = false
                                OfflineRuntime.enqueueAndSync(OpCommand.Pause(op.id, reason, shown.status))
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Elapsed (HH:MM:SS) from `actualStart` and remaining window from `plannedEnd`. Hidden when data is missing. */
@Composable
private fun TimerCard(op: OperationDto, zone: java.time.ZoneId) {
    val start = TimeFormat.parseInstant(op.actualStart, zone)
    val end = TimeFormat.parseInstant(op.plannedEnd, zone)
    if (start == null && end == null) return
    val running = op.canPause()
    var nowMs by remember(op.id, op.status) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(op.id, op.status) {
        while (running) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val now = Instant.ofEpochMilli(nowMs)
    // PAUSED/COMPLETED: freeze at actualEnd if the server has one, else at load time.
    val elapsedUntil = when {
        running -> now
        op.status.equals("COMPLETED", ignoreCase = true) ||
            op.status.equals("PAUSED", ignoreCase = true) -> TimeFormat.parseInstant(op.actualEnd, zone) ?: now
        else -> now
    }
    val elapsed = start?.let { TimeFormat.formatElapsed(Duration.between(it, elapsedUntil).seconds) }
    val remaining = end?.let { TimeFormat.formatRemaining(it, now) }
    val remainingLabel = if (end != null) remaining ?: S.t("run.windowClosed") else null
    val remainingColor = when {
        end == null -> MaterialTheme.colorScheme.onSurface
        remaining == null -> AgOsColors.crit
        Duration.between(now, end) < Duration.ofHours(1) -> AgOsColors.warn
        else -> MaterialTheme.colorScheme.onSurface
    }
    AgCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (elapsed != null) {
                Column(
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${S.t("run.elapsed")}: $elapsed"
                        if (running) liveRegion = LiveRegionMode.Polite
                    },
                ) {
                    Text(S.t("run.elapsed"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(elapsed, style = MonoTimer, color = AgOsColors.g900)
                }
            }
            if (remainingLabel != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(S.t("run.closesIn"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(remainingLabel, style = MaterialTheme.typography.headlineSmall, color = remainingColor)
                    TimeFormat.clock(op.plannedEnd, zone)?.let {
                        Text(it, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
