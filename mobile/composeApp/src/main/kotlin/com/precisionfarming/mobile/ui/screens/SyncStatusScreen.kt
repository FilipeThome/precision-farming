package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.TimeFormat
import com.precisionfarming.mobile.data.formatNumber
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.data.offline.QueuedCommand
import com.precisionfarming.mobile.data.offline.SyncState
import com.precisionfarming.mobile.data.syncDeviceId
import com.precisionfarming.mobile.data.syncPull
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.AgCard
import com.precisionfarming.mobile.ui.components.EntityTile
import com.precisionfarming.mobile.ui.components.KpiCard
import com.precisionfarming.mobile.ui.components.ScreenHeader
import com.precisionfarming.mobile.ui.components.StatusTag
import com.precisionfarming.mobile.ui.components.TagTone
import com.precisionfarming.mobile.ui.theme.AgOsColors
import com.precisionfarming.mobile.ui.theme.MonoSmall
import kotlinx.coroutines.launch
import java.time.ZoneId

/** Queue-backed sync screen: real `QueuedCommand`s only; "Sincronizar" = forced flush + `/sync/pull`. */
@Composable
fun SyncStatusScreen(onBack: () -> Unit) {
    val queue by OfflineRuntime.queue.state.collectAsState()
    val online by OfflineRuntime.connectivity.online.collectAsState()
    var syncing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val zone = ZoneId.systemDefault()

    fun syncNow() {
        if (syncing) return
        syncing = true
        message = null
        scope.launch {
            val result = runCatching {
                OfflineRuntime.flushNow()
                syncPull(syncDeviceId())
            }
            result.onSuccess { OfflineRuntime.queue.recordSync() }
                .onFailure { message = "${S.t("sync.unavailable")}: ${it.message ?: S.t("common.error")}" }
            syncing = false
        }
    }

    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(S.t("sync.title"), onBack) {
            OutlinedButton(
                onClick = { syncNow() },
                enabled = !syncing,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(
                    if (syncing) S.t("sync.syncing") else S.t("sync.now"),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(S.t("sync.pending"), queue.pendingCount.toString(), modifier = Modifier.weight(1f))
            KpiCard(
                S.t("sync.failed"),
                queue.failedCount.toString(),
                modifier = Modifier.weight(1f),
                valueColor = if (queue.failedCount > 0) AgOsColors.crit else MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                S.t("sync.connection"),
                if (online) S.t("strip.online") else S.t("strip.offlineShort"),
                modifier = Modifier.weight(1f),
                valueColor = if (online) AgOsColors.ok else AgOsColors.n700,
            )
            val last = TimeFormat.clockWithSeconds(queue.lastSyncAt, zone)
            if (last != null) {
                KpiCard(S.t("sync.lastSync"), last, modifier = Modifier.weight(1f))
            }
        }
        message?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        val items = queue.items.sortedByDescending { it.createdAt }
        if (items.isEmpty()) {
            AgCard(Modifier.fillMaxWidth()) {
                Text(
                    S.t("sync.empty"),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AgCard(Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, cmd ->
                    if (index > 0) HorizontalDivider(color = AgOsColors.n100)
                    QueueRow(cmd, zone, onDismiss = { OfflineRuntime.queue.dismiss(cmd.clientOperationId) })
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.heightIn(max = 14.dp))
            Text(
                S.t("sync.footnote"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun QueueRow(cmd: QueuedCommand, zone: ZoneId, onDismiss: () -> Unit) {
    val (icon, tone) = when (cmd.state) {
        SyncState.PENDING -> Icons.Outlined.Schedule to TagTone.Neutral
        SyncState.SYNCING -> Icons.Filled.Refresh to TagTone.Info
        SyncState.SYNCED -> Icons.Filled.Check to TagTone.Ok
        SyncState.FAILED -> Icons.Filled.Close to TagTone.Crit
    }
    val title = listOfNotNull(
        S.t("sync.cmd.${cmd.type.name}"),
        cmd.reason?.let { "“$it”" },
        cmd.actualLiters?.let { "${formatNumber(it)} L" },
        TimeFormat.clock(cmd.createdAt, zone),
    ).joinToString(" · ")
    val stateLabel = S.t("sync.state.${cmd.state.name}")
    val sub = when (cmd.state) {
        SyncState.SYNCED -> listOfNotNull(stateLabel, TimeFormat.clockWithSeconds(cmd.syncedAt, zone)).joinToString(" ")
        SyncState.FAILED -> listOfNotNull(stateLabel, cmd.lastError).joinToString(" · ")
        SyncState.PENDING -> listOfNotNull(
            stateLabel,
            if (cmd.attempts > 0) S.t("sync.attempts", "n" to cmd.attempts.toString()) else null,
            cmd.lastError,
        ).joinToString(" · ")
        SyncState.SYNCING -> stateLabel
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $sub" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityTile(icon, size = 34.dp, tone = tone)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = if (cmd.state == SyncState.FAILED) AgOsColors.crit else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(S.t("sync.opId", "id" to cmd.operationId.take(8)), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(cmd.clientOperationId.take(4) + "…", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (cmd.state == SyncState.FAILED) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(S.t("sync.dismiss")) }
            } else {
                StatusTag(stateLabel, tone = tone)
            }
        }
    }
}
