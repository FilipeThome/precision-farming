package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun KpiCard(
    label: String,
    value: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val description = contentDescription ?: listOfNotNull(label, value, hint).joinToString(". ")
    val cardModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = true) {
            this.contentDescription = description
            if (onClick != null) role = Role.Button
        }
    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier) {
            KpiBody(label, value, hint)
        }
    } else {
        Card(cardModifier) {
            KpiBody(label, value, hint)
        }
    }
}

@Composable
private fun KpiBody(label: String, value: String, hint: String?) {
    Column(Modifier.padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.headlineSmall)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}
