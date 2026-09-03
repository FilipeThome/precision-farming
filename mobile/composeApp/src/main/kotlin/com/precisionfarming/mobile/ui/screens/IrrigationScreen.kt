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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.IrrigationAssetDto
import com.precisionfarming.mobile.data.IrrigationRecommendationDto
import com.precisionfarming.mobile.data.irrigationAssets
import com.precisionfarming.mobile.data.irrigationRecommendations
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun IrrigationScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var assets by remember { mutableStateOf<LoadState<IrrigationAssetDto>>(LoadState.Loading) }
    var recs by remember { mutableStateOf<LoadState<IrrigationRecommendationDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId
            if (tab == 0) {
                assets = LoadState.Loading
                assets = runCatching { irrigationAssets(farmId) }.toLoadState()
            } else {
                recs = LoadState.Loading
                recs = runCatching { irrigationRecommendations(farmId) }.toLoadState()
            }
        }
    }
    LaunchedEffect(tab, FarmFilter.farmId, LocaleStore.locale) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text(S.t("common.back")) }
        Text(S.t("irrigation.title"))
        SectionTabs(
            labels = listOf(S.t("irrigation.assets"), S.t("irrigation.recs")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        if (tab == 0) {
            LoadedList(assets) { a ->
                buildString {
                    append(listOfNotNull(a.name, a.type, a.status).joinToString(" · ").ifBlank { a.id })
                    a.capacityMmH?.let { append(" · ${it}mm/h") }
                }
            }
        } else {
            LoadedList(recs) { r ->
                buildString {
                    append(r.id.take(8))
                    r.recommendedMm?.let { append(" · ${it}mm") }
                    r.status?.let { append(" · $it") }
                    r.reason?.let { append(" · $it") }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
