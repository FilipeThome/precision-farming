package com.precisionfarming.mobile.ui.inspectors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.InspectNav
import com.precisionfarming.mobile.data.ackAlert
import com.precisionfarming.mobile.data.formatWhen
import com.precisionfarming.mobile.data.isOpen
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import kotlinx.coroutines.launch

@Composable
fun AlertInspector(
    alert: AlertDto,
    onChanged: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${DomainLabels.label(alert.severity)} · ${DomainLabels.label(alert.status)}")
        alert.message?.takeIf { it.isNotBlank() }?.let { Text(DomainLabels.label(it)) }
        Text(formatWhen(alert.createdAt))
        if (!alert.entityType.isNullOrBlank()) {
            Text("${S.t("alerts.relatedEntity")}: ${DomainLabels.label(alert.entityType)}")
        }
        if (alert.entityType.equals("MACHINE", ignoreCase = true) && !alert.entityId.isNullOrBlank()) {
            TextButton(
                onClick = {
                    if (!alert.farmId.isNullOrBlank()) FarmFilter.farmId = alert.farmId
                    onOpen(InspectNav.href(InspectNav.MACHINES, selected = alert.entityId))
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(S.t("alerts.jumpMachine"))
            }
        }
        if (alert.entityType.equals("FIELD", ignoreCase = true) && !alert.entityId.isNullOrBlank()) {
            TextButton(
                onClick = {
                    if (!alert.farmId.isNullOrBlank()) FarmFilter.farmId = alert.farmId
                    onOpen(InspectNav.href(InspectNav.FIELDS, selected = alert.entityId))
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(S.t("alerts.jumpField"))
            }
        }
        msg?.let { Text(it) }
        if (alert.isOpen()) {
            Button(
                onClick = {
                    scope.launch {
                        runCatching { ackAlert(alert.id) }
                            .onSuccess { onChanged() }
                            .onFailure { msg = it.message }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(S.t("alerts.ack")) }
        }
    }
}
