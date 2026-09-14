package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.ui.theme.AgOsColors

/** Icon on a tinted rounded square — replaces photos everywhere (no sample media on the client). */
@Composable
fun EntityTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tone: TagTone = TagTone.Neutral,
    contentDescription: String? = null,
) {
    val c = when (tone) {
        TagTone.Neutral -> TagColors(AgOsColors.g100, AgOsColors.g700)
        else -> tone.colors()
    }
    Box(
        modifier
            .size(size)
            .background(c.bg, RoundedCornerShape(size * 0.27f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.fg, modifier = Modifier.size(size / 2))
    }
}

/** Icon by operation type code (SPRAYING, PLANTING…). Unknown → clipboard. */
fun operationIcon(type: String?): ImageVector = when (type?.trim()?.uppercase()) {
    "SPRAYING", "APPLICATION", "IRRIGATION" -> Icons.Outlined.WaterDrop
    "PLANTING" -> Icons.Outlined.Spa
    "HARVEST" -> Icons.Outlined.Agriculture
    "FERTILIZING" -> Icons.Outlined.Science
    "INSPECTION", "SCOUTING" -> Icons.Outlined.Search
    "MAINTENANCE" -> Icons.Outlined.Build
    "TRANSPORT", "STORAGE" -> Icons.Outlined.Inventory2
    else -> Icons.AutoMirrored.Outlined.Assignment
}

/** Icon by entity kind for list rows (farm, field, machine, item, alert…). */
fun entityIcon(kind: String): ImageVector = when (kind.uppercase()) {
    "FARM" -> Icons.Outlined.Landscape
    "FIELD" -> Icons.Outlined.Grass
    "MACHINE" -> Icons.Outlined.Agriculture
    "ITEM", "INVENTORY" -> Icons.Outlined.Inventory2
    "ALERT" -> Icons.Outlined.Notifications
    else -> Icons.AutoMirrored.Outlined.Assignment
}

/** Tile tone by operation status: done → ok, running → teal, paused → warn, planned → neutral. */
fun toneForOperationStatus(status: String?): TagTone = when (status?.uppercase()) {
    "COMPLETED" -> TagTone.Ok
    "IN_PROGRESS" -> TagTone.Teal
    "PAUSED" -> TagTone.Warn
    else -> TagTone.Neutral
}
