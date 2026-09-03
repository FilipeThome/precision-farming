package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
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
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.completeOp
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.data.pauseOp
import com.precisionfarming.mobile.data.startOp
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun OpsScreen() {
    var state by remember { mutableStateOf<LoadState<OperationDto>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    val defaultReason = S.t("ops.pauseReasonDefault")
    var pauseReason by remember(LocaleStore.locale) { mutableStateOf(defaultReason) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { operations(FarmFilter.farmId) }.toLoadState()
        }
    }
    LaunchedEffect(FarmFilter.farmId, LocaleStore.locale) { reload() }
    LazyColumn(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text(S.t("ops.title")) }
        item {
            OutlinedTextField(
                pauseReason,
                { pauseReason = it },
                label = { Text(S.t("ops.pauseReason")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        msg?.let { message -> item { Text(message) } }
        when (val s = state) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("common.error")}: ${s.message}") }
            is LoadState.Ok -> {
                if (s.items.isEmpty()) item { Text(S.t("common.empty")) }
                items(s.items, key = { it.id }) { op ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${op.type} · ${op.status}")
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { startOp(op.id) }.onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("ops.start")) }
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { pauseOp(op.id, pauseReason) }.onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("ops.pause")) }
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { completeOp(op.id) }.onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("ops.complete")) }
                    }
                }
            }
        }
        item { TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) } }
    }
}
