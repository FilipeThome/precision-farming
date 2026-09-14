package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.TimeFormat
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.theme.AgOsColors
import com.precisionfarming.mobile.ui.theme.MonoSmall
import java.time.Duration
import java.time.Instant

/** Mono chip with the age of a timestamp. Renders nothing when the timestamp is missing/unparsable. */
@Composable
fun FreshnessChip(
    iso: String?,
    now: Instant = Instant.now(),
    staleAfter: Duration = Duration.ofHours(24),
    modifier: Modifier = Modifier,
) {
    val age = TimeFormat.formatAge(iso, now) ?: return
    val stale = TimeFormat.isStale(iso, now, staleAfter)
    val bg = if (stale) AgOsColors.warnBg else AgOsColors.n100
    val fg = if (stale) AgOsColors.warn else AgOsColors.n700
    val label = S.t("freshness.updated", "age" to age)
    Row(
        modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        Text(age, style = MonoSmall, color = fg)
    }
}
