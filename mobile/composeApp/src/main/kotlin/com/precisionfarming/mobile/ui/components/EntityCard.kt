package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * List row card. [status] renders as a [StatusTag] when it looks like a backend code, else as plain text.
 * [icon] renders an [EntityTile] on the left.
 */
@Composable
fun EntityCard(
    headline: String,
    supporting: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    status: String? = null,
    statusTone: TagTone? = null,
    icon: ImageVector? = null,
    iconTone: TagTone = TagTone.Neutral,
    trailing: (@Composable () -> Unit)? = null,
) {
    val description = listOfNotNull(headline, supporting, status).joinToString(". ")
    val cardModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = true) {
            contentDescription = description
            if (onClick != null) role = Role.Button
        }
    AgCard(modifier = cardModifier, onClick = onClick) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) EntityTile(icon, tone = iconTone)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(headline, style = MaterialTheme.typography.titleMedium)
                if (supporting.isNotBlank()) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!status.isNullOrBlank()) {
                    StatusTag(status, tone = statusTone ?: TagTone.Neutral, modifier = Modifier.padding(top = 4.dp))
                }
            }
            trailing?.invoke()
        }
    }
}
