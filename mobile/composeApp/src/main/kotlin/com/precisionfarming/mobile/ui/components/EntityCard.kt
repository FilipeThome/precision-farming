package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun EntityCard(
    headline: String,
    supporting: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    status: String? = null,
) {
    val description = listOfNotNull(headline, supporting, status).joinToString(". ")
    val cardModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = true) {
            contentDescription = description
            if (onClick != null) role = Role.Button
        }
    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier, colors = CardDefaults.cardColors()) {
            EntityCardBody(headline, supporting, status)
        }
    } else {
        Card(modifier = cardModifier, colors = CardDefaults.cardColors()) {
            EntityCardBody(headline, supporting, status)
        }
    }
}

@Composable
private fun EntityCardBody(headline: String, supporting: String, status: String?) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(headline, style = MaterialTheme.typography.titleMedium)
        if (supporting.isNotBlank()) {
            Text(supporting, style = MaterialTheme.typography.bodyMedium)
        }
        if (!status.isNullOrBlank()) {
            Text(status, style = MaterialTheme.typography.labelLarge)
        }
    }
}
