package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class InspectorKpiItem(val label: String, val value: String, val hint: String? = null)

@Composable
fun InspectorKpis(items: List<InspectorKpiItem>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "${item.label}: ${item.value}"
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.label, style = MaterialTheme.typography.labelSmall)
                            Text(item.value, style = MaterialTheme.typography.titleMedium)
                            if (item.hint != null) {
                                Text(item.hint, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (row.size == 1) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
