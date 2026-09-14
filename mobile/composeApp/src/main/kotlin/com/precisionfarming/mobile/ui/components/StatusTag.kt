package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.ui.theme.AgOsColors

/** Tag tones from agos.css `.tag.*`. */
enum class TagTone { Crit, Warn, Ok, Info, Neutral, Teal, Outline, OnDark }

data class TagColors(val bg: Color, val fg: Color, val border: Color? = null)

fun TagTone.colors(): TagColors = when (this) {
    TagTone.Crit -> TagColors(AgOsColors.critBg, AgOsColors.crit)
    TagTone.Warn -> TagColors(AgOsColors.warnBg, AgOsColors.warn)
    TagTone.Ok -> TagColors(AgOsColors.okBg, AgOsColors.ok)
    TagTone.Info -> TagColors(AgOsColors.infoBg, AgOsColors.info)
    TagTone.Neutral -> TagColors(AgOsColors.n100, AgOsColors.n700)
    TagTone.Teal -> TagColors(AgOsColors.t100, AgOsColors.t700)
    TagTone.Outline -> TagColors(Color.Transparent, AgOsColors.n700, AgOsColors.n300)
    TagTone.OnDark -> TagColors(AgOsColors.heroChip, AgOsColors.n0)
}

/** Maps backend status/severity codes onto a tag tone. Unknown codes are neutral. */
fun toneForStatus(code: String?): TagTone = when (code?.trim()?.uppercase()) {
    "IN_PROGRESS", "OPERATING", "RUNNING" -> TagTone.Teal
    "COMPLETED", "APPROVED", "OK", "SYNCED", "DELIVERED", "FAVORABLE", "HEALTHY", "ACKED", "READY" -> TagTone.Ok
    "PAUSED", "WARNING", "MARGINAL", "MAINTENANCE", "QUEUED", "OPEN", "DRAFT", "MEDIUM" -> TagTone.Warn
    "CRITICAL", "FAILED", "UNFAVORABLE", "BLOCKED", "HIGH" -> TagTone.Crit
    "INFO", "SYNCING", "DISPATCHED" -> TagTone.Info
    else -> TagTone.Neutral
}

@Composable
fun StatusTag(
    text: String,
    tone: TagTone = TagTone.Neutral,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    large: Boolean = false,
) {
    val c = tone.colors()
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier
            .background(c.bg, shape)
            .then(if (c.border != null) Modifier.border(1.dp, c.border, shape) else Modifier)
            .padding(horizontal = if (large) 10.dp else 8.dp, vertical = if (large) 4.dp else 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.fg, modifier = Modifier.size(if (large) 14.dp else 12.dp))
        Text(
            text,
            color = c.fg,
            style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}
