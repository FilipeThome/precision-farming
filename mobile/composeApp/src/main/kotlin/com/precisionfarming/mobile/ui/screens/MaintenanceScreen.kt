package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.MaintenanceWorkOrderDto
import com.precisionfarming.mobile.data.completeWorkOrder
import com.precisionfarming.mobile.data.maintenanceWorkOrders
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<MaintenanceWorkOrderDto>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { maintenanceWorkOrders(FarmFilter.farmId) }.toLoadState()
        }
    }
    LaunchedEffect(FarmFilter.farmId, LocaleStore.locale) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text(S.t("common.back")) }
        Text(S.t("maintenance.title"))
        msg?.let { Text(it) }
        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                if (s.items.isEmpty()) Text(S.t("common.empty"))
                s.items.forEach { wo ->
                    Text(
                        listOfNotNull(wo.title, wo.priority, wo.status, wo.createdAt)
                            .joinToString(" · ")
                            .ifBlank { wo.id },
                    )
                    if (wo.status?.equals("COMPLETED", ignoreCase = true) != true) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { completeWorkOrder(wo.id) }.onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("maintenance.complete")) }
                    }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
