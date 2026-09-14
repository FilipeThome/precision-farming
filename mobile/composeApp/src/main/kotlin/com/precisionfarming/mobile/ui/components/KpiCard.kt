package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** `.mini .mcard`: big display number + small label (+ optional hint). */
@Composable
fun KpiCard(
    label: String,
    value: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val description = contentDescription ?: listOfNotNull(label, value, hint).joinToString(". ")
    val cardModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = true) {
            this.contentDescription = description
            if (onClick != null) role = Role.Button
        }
    AgCard(modifier = cardModifier, onClick = onClick) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
