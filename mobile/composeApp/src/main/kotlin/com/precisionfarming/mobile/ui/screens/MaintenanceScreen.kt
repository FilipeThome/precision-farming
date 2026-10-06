package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.MaintenanceWorkOrderDto
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.WorkOrderCreate
import com.precisionfarming.mobile.data.canWriteFleet
import com.precisionfarming.mobile.data.completeWorkOrder
import com.precisionfarming.mobile.data.createWorkOrder
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.maintenanceWorkOrders
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.EntityFormSheet
import com.precisionfarming.mobile.ui.components.FormField
import com.precisionfarming.mobile.ui.components.FormOption
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.ScreenHeader
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<MaintenanceWorkOrderDto>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var machineChoices by remember { mutableStateOf(listOf<FormOption>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canWrite = canWriteFleet(Session.role)
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { maintenanceWorkOrders(FarmFilter.farmId.value) }.toLoadState()
        }
    }
    LaunchedEffect(LocalFarmId.current, LocalAppLocale.current) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(S.t("maintenance.title"), onBack, actions = {
            if (canWrite) {
                TextButton(onClick = {
                    creating = true
                    error = null
                    values = mapOf("machineId" to "", "title" to "", "priority" to "MEDIUM")
                    scope.launch {
                        val rows = runCatching { machines(FarmFilter.farmId.value) }.getOrDefault(emptyList())
                        machineChoices = rows.map { FormOption(it.id, it.name) }
                        val first = machineChoices.firstOrNull()?.value
                        if (first != null) values = values + ("machineId" to first)
                    }
                }) { Text(S.t("form.new")) }
            }
        })
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
    if (creating) {
        EntityFormSheet(
            title = S.t("form.new"),
            fields = listOf(
                FormField("machineId", S.t("form.field.machine"), options = machineChoices),
                FormField("title", S.t("form.field.title")),
                FormField("priority", S.t("form.field.priority")),
            ),
            values = values,
            onChange = { k, v -> values = values + (k to v) },
            pending = pending,
            error = error,
            onDismiss = { creating = false },
            onSave = {
                val farmId = FarmFilter.farmId.value
                if (farmId.isNullOrBlank()) {
                    error = S.t("form.needFarm")
                    return@EntityFormSheet
                }
                scope.launch {
                    pending = true
                    runCatching {
                        createWorkOrder(
                            WorkOrderCreate(
                                farmId,
                                values["machineId"].orEmpty(),
                                values["title"].orEmpty(),
                                values["priority"].orEmpty(),
                            ),
                        )
                    }.onSuccess {
                        creating = false
                        reload()
                    }.onFailure { error = it.message }
                    pending = false
                }
            },
        )
    }
}
