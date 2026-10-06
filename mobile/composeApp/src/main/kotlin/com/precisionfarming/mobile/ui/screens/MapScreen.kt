package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.viewinterop.AndroidView
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.MapLayerDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.centroidOfGeometry
import com.precisionfarming.mobile.data.fieldLeafletHtml
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.mapLayers
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.FieldStatusList
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.ScreenHeader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class MapBundle(
    val fields: List<FieldDto>,
    val operations: List<OperationDto>,
    val layers: List<MapLayerDto>,
    val partialError: Boolean,
)

@Composable
fun MapScreen() {
    var state by remember { mutableStateOf<LoadState<MapBundle>>(LoadState.Loading) }
    var layersExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching {
                val farmId = FarmFilter.farmId.value
                coroutineScope {
                    val fieldJob = async { runCatching { fields(farmId) } }
                    val opJob = async { runCatching { operations(farmId) } }
                    val layerJob = async { runCatching { mapLayers(farmId) } }
                    val field = fieldJob.await()
                    val op = opJob.await()
                    val layer = layerJob.await()
                    val partial = listOf(field, op, layer).any { it.isFailure }
                    if (field.isFailure && op.isFailure && layer.isFailure) {
                        error(field.exceptionOrNull()?.message ?: S.t("common.error"))
                    }
                    MapBundle(
                        fields = field.getOrDefault(emptyList()),
                        operations = op.getOrDefault(emptyList()),
                        layers = layer.getOrDefault(emptyList()),
                        partialError = partial,
                    )
                }
            }.fold(
                onSuccess = { LoadState.Ok(listOf(it)) },
                onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
            )
        }
    }
    LaunchedEffect(LocalFarmId.current, LocalAppLocale.current) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(S.t("map.title"))
        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                val b = s.items.first()
                if (b.partialError) {
                    Text(
                        S.t("common.partialError"),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val points = b.fields.mapNotNull { field ->
                    val centroid = centroidOfGeometry(field.geometry) ?: return@mapNotNull null
                    (field.name ?: field.id) to centroid
                }
                if (points.isEmpty()) {
                    Text(
                        S.t("map.noGeometry"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val html = fieldLeafletHtml(points)
                    // Interim WebView until the Google Maps SDK (ADR-002).
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                tag = html
                                loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            if (webView.tag != html) {
                                webView.tag = html
                                webView.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null)
                            }
                        },
                        onRelease = { it.destroy() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                    )
                }
                FieldStatusList(fields = b.fields, operations = b.operations)
                TextButton(onClick = { layersExpanded = !layersExpanded }) {
                    Text(if (layersExpanded) S.t("map.layers.hide") else S.t("map.layers"))
                }
                if (layersExpanded) {
                    LoadedList(LoadState.Ok(b.layers)) { layer ->
                        listOfNotNull(layer.name, layer.kind, layer.status).joinToString(" · ").ifBlank { layer.id }
                    }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
