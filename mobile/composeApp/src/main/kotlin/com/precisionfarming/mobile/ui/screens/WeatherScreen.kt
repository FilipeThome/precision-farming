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
import com.precisionfarming.mobile.data.WeatherForecastDto
import com.precisionfarming.mobile.data.WeatherWindowDto
import com.precisionfarming.mobile.data.forecast
import com.precisionfarming.mobile.data.weatherWindows
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch
import com.precisionfarming.mobile.ui.components.ScreenHeader

@Composable
fun WeatherScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var forecastState by remember { mutableStateOf<LoadState<WeatherForecastDto>>(LoadState.Loading) }
    var windowState by remember { mutableStateOf<LoadState<WeatherWindowDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId
            if (tab == 0) {
                forecastState = LoadState.Loading
                forecastState = runCatching { forecast(farmId) }.toLoadState()
            } else {
                windowState = LoadState.Loading
                windowState = runCatching { weatherWindows(farmId) }.toLoadState()
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
        ScreenHeader(S.t("weather.title"), onBack)
        SectionTabs(
            labels = listOf(S.t("tab.forecast"), S.t("tab.windows")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        if (tab == 0) {
            LoadedList(forecastState) { f ->
                listOfNotNull(
                    f.forecastAt,
                    f.temperatureMin?.let { "min $it" },
                    f.temperatureMax?.let { "max $it" },
                    f.rainMm?.let { "${it}mm" },
                    f.sprayingWindow,
                ).joinToString(" · ").ifBlank { f.id }
            }
        } else {
            LoadedList(windowState) { w ->
                listOfNotNull(w.windowType, w.rating, w.startAt, w.notes)
                    .joinToString(" · ")
                    .ifBlank { w.id }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
