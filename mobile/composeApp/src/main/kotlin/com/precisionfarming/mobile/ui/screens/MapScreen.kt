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
import com.precisionfarming.mobile.data.MapLayerDto
import com.precisionfarming.mobile.data.mapLayers
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun MapScreen() {
    var state by remember { mutableStateOf<LoadState<MapLayerDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { mapLayers(FarmFilter.farmId) }.toLoadState()
        }
    }
    LaunchedEffect(FarmFilter.farmId, LocaleStore.locale) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(S.t("map.title"))
        Text(S.t("map.noKey"))
        Text(S.t("map.layers"))
        LoadedList(state) { layer ->
            listOfNotNull(layer.name, layer.kind, layer.status).joinToString(" · ").ifBlank { layer.id }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
