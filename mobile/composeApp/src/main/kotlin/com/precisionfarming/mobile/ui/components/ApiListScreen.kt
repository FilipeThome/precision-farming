package com.precisionfarming.mobile.ui.components

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.precisionfarming.mobile.i18n.S
import kotlinx.coroutines.launch
import com.precisionfarming.mobile.ui.components.ScreenHeader

@Composable
fun ApiListScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    load: suspend () -> List<String>,
) {
    var state by remember(title) { mutableStateOf<LoadState<String>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { load() }.toLoadState()
        }
    }
    LaunchedEffect(title, LocalFarmId.current, LocalAppLocale.current) { reload() }
    LazyColumn(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader(title, onBack) }
        when (val s = state) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("common.error")}: ${s.message}") }
            is LoadState.Ok -> {
                if (s.items.isEmpty()) {
                    item { Text(S.t("common.empty")) }
                } else {
                    items(s.items) { Text(it) }
                }
            }
        }
        item { TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) } }
    }
}
