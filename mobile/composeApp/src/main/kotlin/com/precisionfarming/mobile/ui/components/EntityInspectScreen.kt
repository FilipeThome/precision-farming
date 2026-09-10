package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import kotlinx.coroutines.launch

@Composable
fun <T> EntityInspectScreen(
    title: String,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    load: suspend () -> List<T>,
    idOf: (T) -> String,
    headline: (T) -> String,
    supporting: (T) -> String,
    inspector: @Composable (T) -> Unit,
    onBack: (() -> Unit)? = null,
    statusOf: (T) -> String? = { null },
    sheetTitle: (T) -> String = headline,
    sheetSubtitle: (T) -> String? = { null },
    extraKeys: Any? = null,
    header: @Composable () -> Unit = {},
) {
    var state by remember { mutableStateOf<LoadState<T>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching { load() }.toLoadState()
        }
    }
    LaunchedEffect(title, extraKeys, FarmFilter.farmId, LocaleStore.locale) { reload() }
    val items = (state as? LoadState.Ok)?.items.orEmpty()
    val selected = items.byId(selectedId, idOf)
    val sheetOpen = !selectedId.isNullOrBlank()
    LazyColumn(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            item { TextButton(onClick = onBack) { Text(S.t("common.back")) } }
        }
        item { Text(title, style = MaterialTheme.typography.titleLarge) }
        item { header() }
        when (val s = state) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("common.error")}: ${s.message}") }
            is LoadState.Ok -> {
                if (s.items.isEmpty()) item { Text(S.t("common.empty")) }
                items(s.items, key = { idOf(it) }) { row ->
                    EntityCard(
                        headline = headline(row),
                        supporting = supporting(row),
                        status = statusOf(row),
                        onClick = { onSelect(idOf(row)) },
                    )
                }
            }
        }
        item { TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) } }
    }
        if (sheetOpen) {
        val row = selected
        DetailSheet(
            title = row?.let(sheetTitle) ?: S.t("inspector.notFound"),
            subtitle = row?.let(sheetSubtitle),
            found = row != null,
            onDismiss = onClearSelected,
        ) {
            if (row != null) inspector(row)
        }
    }
}
