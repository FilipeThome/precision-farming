package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.EntityNames
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.FieldState
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.countByState
import com.precisionfarming.mobile.data.fieldStates
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S

@Composable
fun FieldStatusList(
    fields: List<FieldDto>,
    operations: List<OperationDto>,
    modifier: Modifier = Modifier,
) {
    val states = fieldStates(fields, operations)
    val counts = countByState(states)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(S.t("tower.map.layer.status"), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                FieldState.progress to "tower.map.legend.progress",
                FieldState.blocked to "tower.map.legend.paused",
                FieldState.planned to "tower.map.legend.planned",
                FieldState.done to "tower.map.legend.done",
                FieldState.stale to "tower.map.legend.stale",
                FieldState.none to "tower.map.legend.none",
            ).forEach { (state, key) ->
                val n = counts[state] ?: 0
                if (n > 0 || state == FieldState.progress) {
                    Text(
                        "${S.t(key)} $n",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (fields.isEmpty()) {
            Text(S.t("map.status.empty"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            fields.forEach { field ->
                val state = states[field.id] ?: FieldState.none
                EntityCard(
                    headline = EntityNames.nameOf(field.id) ?: field.name ?: field.id,
                    supporting = listOfNotNull(field.crop?.let { DomainLabels.label(it) }, field.areaHa?.let { "${it} ha" })
                        .joinToString(" · "),
                    status = S.t("map.status.${state.name}"),
                )
            }
        }
    }
}
