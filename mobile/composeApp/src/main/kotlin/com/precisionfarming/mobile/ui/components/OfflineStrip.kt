package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.TimeFormat
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.theme.AgOsColors
import java.time.Instant

/** Reads runtime state and renders [OfflineStrip]. Safe to call before the runtime is initialised. */
@Composable
fun OfflineStripFromRuntime(modifier: Modifier = Modifier) {
    if (!OfflineRuntime.isReady) return
    val online by OfflineRuntime.connectivity.online.collectAsState()
    val queue by OfflineRuntime.queue.state.collectAsState()
    OfflineStrip(online = online, pendingCount = queue.pendingCount, lastSyncAt = queue.lastSyncAt, modifier = modifier)
}

/** Calm connectivity strip: only connectivity, pending queue count and last sync time (when one exists). */
@Composable
fun OfflineStrip(
    online: Boolean,
    pendingCount: Int,
    lastSyncAt: String?,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now(),
) {
    val bg = if (online) AgOsColors.g100 else AgOsColors.n800
    val fg = if (online) AgOsColors.g800 else AgOsColors.onDarkStrip
    val stateText = if (online) S.t("strip.online") else S.t("strip.offline")
    val pendingText = if (pendingCount > 0) S.t("strip.pending", "n" to pendingCount.toString()) else null
    val syncText = TimeFormat.formatAge(lastSyncAt, now)?.let { S.t("strip.lastSync", "when" to it) }
    val description = listOfNotNull(stateText, pendingText, syncText).joinToString(" · ")
    Row(
        modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = description
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (online) Icons.Outlined.Wifi else Icons.Outlined.WifiOff, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(stateText, color = fg, style = MaterialTheme.typography.labelMedium)
        if (pendingText != null) {
            Separator(fg)
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Text(pendingText, color = fg, style = MaterialTheme.typography.labelMedium)
        }
        if (syncText != null) {
            Separator(fg)
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Text(syncText, color = fg, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Separator(color: Color) {
    Box(Modifier.width(1.dp).height(12.dp).background(color.copy(alpha = 0.3f)))
}
