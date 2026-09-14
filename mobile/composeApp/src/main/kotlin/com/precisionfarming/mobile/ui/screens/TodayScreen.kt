package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.InspectNav
import com.precisionfarming.mobile.data.InventoryItemDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.MeDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.TimeFormat
import com.precisionfarming.mobile.data.TodayOps
import com.precisionfarming.mobile.data.WeatherWindowDto
import com.precisionfarming.mobile.data.alerts
import com.precisionfarming.mobile.data.canPause
import com.precisionfarming.mobile.data.canStart
import com.precisionfarming.mobile.data.withQueuedStatus
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.formatNumber
import com.precisionfarming.mobile.data.inventory
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.me
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.data.offline.OpCommand
import com.precisionfarming.mobile.data.offline.SyncState
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.data.weatherWindows
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.AgCard
import com.precisionfarming.mobile.ui.components.EntityTile
import com.precisionfarming.mobile.ui.components.KpiCard
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.StatusTag
import com.precisionfarming.mobile.ui.components.TagTone
import com.precisionfarming.mobile.ui.components.operationIcon
import com.precisionfarming.mobile.ui.components.toneForOperationStatus
import com.precisionfarming.mobile.ui.components.toneForStatus
import com.precisionfarming.mobile.ui.theme.AgOsColors
import com.precisionfarming.mobile.ui.theme.MonoSmall
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private data class TodayBundle(
    val me: MeDto?,
    val farms: List<FarmDto>,
    val operations: List<OperationDto>,
    val alerts: List<AlertDto>,
    val fields: List<FieldDto>,
    val machines: List<MachineDto>,
    val items: List<InventoryItemDto>,
    val windows: List<WeatherWindowDto>,
)

/** Home tab: greeting, next actionable order, mini KPIs and today's orders — all from gateway data. */
@Composable
fun TodayScreen(onOpen: (String) -> Unit, onOpenRun: (String) -> Unit) {
    var state by remember { mutableStateOf<LoadState<TodayBundle>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching {
                val farmId = FarmFilter.farmId
                coroutineScope {
                    val meJob = async { runCatching { me() }.getOrNull() }
                    val farmJob = async { farms() }
                    val opJob = async { operations(farmId) }
                    val alertJob = async { alerts(farmId) }
                    val fieldJob = async { runCatching { fields(farmId) }.getOrDefault(emptyList()) }
                    val machineJob = async { runCatching { machines(farmId) }.getOrDefault(emptyList()) }
                    val itemJob = async { runCatching { inventory(farmId) }.getOrDefault(emptyList()) }
                    val windowJob = async { runCatching { weatherWindows(farmId) }.getOrDefault(emptyList()) }
                    TodayBundle(
                        me = meJob.await(),
                        farms = farmJob.await(),
                        operations = opJob.await(),
                        alerts = alertJob.await(),
                        fields = fieldJob.await(),
                        machines = machineJob.await(),
                        items = itemJob.await(),
                        windows = windowJob.await(),
                    )
                }
            }.fold(
                onSuccess = { LoadState.Ok(listOf(it)) },
                onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
            )
        }
    }
    LaunchedEffect(FarmFilter.farmId, LocaleStore.locale) { reload() }
    val queue by OfflineRuntime.queue.state.collectAsState()
    // Reload when a queued command for any operation finishes syncing so statuses reflect the server.
    val syncedCount = queue.items.count { it.state == SyncState.SYNCED }
    LaunchedEffect(syncedCount) { if (syncedCount > 0) reload() }

    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val s = state) {
            is LoadState.Loading -> {
                Greeting(null)
                Text(S.t("common.loading"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is LoadState.Err -> {
                Greeting(null)
                Text("${S.t("common.error")}: ${s.message}", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { reload() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S.t("common.refresh")) }
            }
            is LoadState.Ok -> {
                val b = s.items.first()
                val zone = TodayOps.resolveZone(b.farms, FarmFilter.farmId)
                val now = Instant.now()
                Greeting(b.me?.name)
                val ops = b.operations.map { it.withQueuedStatus(queue) }
                val next = TodayOps.nextActionable(ops, zone)
                if (next != null) {
                    HeroCard(
                        op = next,
                        bundle = b,
                        zone = zone,
                        pending = queue.openFor(next.id) != null,
                        onPrimary = {
                            if (next.canStart()) OfflineRuntime.enqueueAndSync(OpCommand.Start(next.id))
                            onOpenRun(next.id)
                        },
                        onOpen = { onOpenRun(next.id) },
                    )
                } else {
                    EmptyCard(S.t("today.noNext"), S.t("today.noNextHint"))
                }
                val kpis = TodayOps.todayKpis(b.operations, b.alerts, now, zone)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(
                        S.t("today.kpi.ops"),
                        if (kpis.opsToday == 0) "—" else kpis.opsToday.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpen(InspectNav.href(InspectNav.OPS)) },
                    )
                    KpiCard(
                        S.t("today.kpi.done"),
                        kpis.completedLabel ?: "—",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpen(InspectNav.href(InspectNav.OPS)) },
                    )
                    KpiCard(
                        S.t("today.kpi.alerts"),
                        kpis.openAlerts.toString(),
                        modifier = Modifier.weight(1f),
                        valueColor = if (kpis.openAlerts > 0) AgOsColors.warn else MaterialTheme.colorScheme.onSurface,
                        onClick = { onOpen(InspectNav.href(InspectNav.ALERTS)) },
                    )
                }
                TodayOrders(
                    ops = TodayOps.operationsToday(b.operations, now, zone),
                    fields = b.fields,
                    zone = zone,
                    onSeeAll = { onOpen(InspectNav.href(InspectNav.OPS)) },
                    onOpenRun = onOpenRun,
                )
                TextButton(onClick = { reload() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S.t("common.refresh")) }
            }
        }
    }
}

@Composable
private fun Greeting(name: String?) {
    val zone = ZoneId.systemDefault()
    val greeting = S.t(TodayOps.greetingKey(Instant.now(), zone))
    Column {
        if (name.isNullOrBlank()) {
            Text(greeting, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        } else {
            Text("$greeting,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        }
    }
}

@Composable
private fun HeroCard(
    op: OperationDto,
    bundle: TodayBundle,
    zone: ZoneId,
    pending: Boolean,
    onPrimary: () -> Unit,
    onOpen: () -> Unit,
) {
    val field = bundle.fields.firstOrNull { it.id == op.fieldId }
    val machine = bundle.machines.firstOrNull { it.id == op.machineId }
    val item = bundle.items.firstOrNull { it.id == op.itemId }
    val title = listOfNotNull(DomainLabels.label(op.type), field?.name?.let { DomainLabels.label(it) }).joinToString(" · ")
    val subtitle = listOfNotNull(
        machine?.name?.let { DomainLabels.label(it) },
        op.areaHa?.let { "${formatNumber(it)} ha" },
    ).joinToString(" · ")
    val window = TimeFormat.window(op.plannedStart, op.plannedEnd, zone)
    val input = if (item != null && op.itemQuantity != null) {
        "${DomainLabels.label(item.name)} · ${formatNumber(op.itemQuantity)} ${item.unit.orEmpty()}".trim()
    } else null
    val weather = TodayOps.weatherRatingFor(op, bundle.windows, zone)
    val running = op.canPause()
    val primaryLabel = when {
        running -> S.t("today.continue")
        op.status.equals("PAUSED", ignoreCase = true) -> S.t("today.continue")
        else -> S.t("today.start")
    }
    AgCard(
        onClick = onOpen,
        containerColor = AgOsColors.g900,
        border = null,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${S.t("today.nextOrder")}. $title" },
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(AgOsColors.g800, AgOsColors.g950)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusTag(
                    if (running) DomainLabels.label(op.status) else S.t("today.nextOrder"),
                    tone = TagTone.OnDark,
                    icon = Icons.Filled.PlayArrow,
                )
                if (pending) StatusTag(S.t("run.pendingSync"), tone = TagTone.Warn)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = AgOsColors.n0)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AgOsColors.heroMuted)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (window != null) HeroMeta(S.t("today.window"), window)
                    if (input != null) HeroMeta(S.t("today.input"), input)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (weather != null) HeroMeta(S.t("today.weather"), DomainLabels.label(weather), tone = toneForStatus(weather))
                    if (op.status.equals("PAUSED", ignoreCase = true) && !op.pauseReason.isNullOrBlank()) {
                        HeroMeta(S.t("ops.pauseReason"), DomainLabels.label(op.pauseReason))
                    }
                }
            }
            Button(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgOsColors.t500, contentColor = AgOsColors.n0),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(primaryLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                S.t("today.offlineHint"),
                style = MaterialTheme.typography.bodySmall,
                color = AgOsColors.heroMuted,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HeroMeta(label: String, value: String, tone: TagTone? = null) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgOsColors.heroMuted)
        if (tone != null) {
            StatusTag(value, tone = tone)
        } else {
            Text(value, style = MaterialTheme.typography.titleSmall, color = AgOsColors.n0)
        }
    }
}

@Composable
private fun TodayOrders(
    ops: List<OperationDto>,
    fields: List<FieldDto>,
    zone: ZoneId,
    onSeeAll: () -> Unit,
    onOpenRun: (String) -> Unit,
) {
    AgCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(S.t("today.myOrders"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            TextButton(onClick = onSeeAll, modifier = Modifier.heightIn(min = 48.dp)) { Text(S.t("today.seeAll")) }
        }
        if (ops.isEmpty()) {
            Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(S.t("today.empty"), style = MaterialTheme.typography.titleSmall)
                Text(S.t("today.emptyHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            ops.forEachIndexed { index, op ->
                if (index > 0) HorizontalDivider(color = AgOsColors.n100)
                val field = fields.firstOrNull { it.id == op.fieldId }
                val title = listOfNotNull(DomainLabels.label(op.type), field?.name?.let { DomainLabels.label(it) }).joinToString(" · ")
                val sub = listOfNotNull(
                    DomainLabels.label(op.status),
                    op.areaHa?.let { "${formatNumber(it)} ha" },
                    TimeFormat.clock(op.plannedEnd, zone)?.let { S.t("today.windowUntil", "time" to it) },
                ).joinToString(" · ")
                val at = TimeFormat.clock(op.plannedStart, zone) ?: "—"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(onClick = { onOpenRun(op.id) })
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "$title. $sub. $at"
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EntityTile(operationIcon(op.type), tone = toneForOperationStatus(op.status))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(at, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, hint: String) {
    AgCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            EntityTile(Icons.AutoMirrored.Outlined.Assignment)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
