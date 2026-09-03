package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.precisionfarming.mobile.data.syncDeviceId
import com.precisionfarming.mobile.data.syncPull
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import kotlinx.coroutines.launch

@Composable
fun SyncStatusScreen(onBack: () -> Unit) {
    var lines by remember { mutableStateOf<LoadState<String>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            lines = LoadState.Loading
            lines = runCatching {
                val res = syncPull(syncDeviceId())
                buildList {
                    add(S.t("sync.online"))
                    res.deviceId?.let { add("deviceId · $it") }
                    add("${S.t("sync.cursor")}: ${res.cursor ?: "—"}")
                    add(S.t("sync.empty"))
                }
            }.fold(
                onSuccess = { LoadState.Ok(it) },
                onFailure = {
                    LoadState.Ok(
                        listOf("${S.t("sync.unavailable")}: ${it.message ?: S.t("common.error")}"),
                    )
                },
            )
        }
    }
    LaunchedEffect(LocaleStore.locale) { reload() }
    LazyColumn(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { TextButton(onClick = onBack) { Text(S.t("common.back")) } }
        item { Text(S.t("sync.title")) }
        when (val s = lines) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("sync.unavailable")}: ${s.message}") }
            is LoadState.Ok -> items(s.items) { Text(it) }
        }
        item { TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) } }
    }
}
