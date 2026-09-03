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
import com.precisionfarming.mobile.data.EsgMetricDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.TraceabilityLotDto
import com.precisionfarming.mobile.data.esg
import com.precisionfarming.mobile.data.traceability
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

@Composable
fun ComplianceScreen(onBack: () -> Unit, onOpenLot: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var lots by remember { mutableStateOf<LoadState<TraceabilityLotDto>>(LoadState.Loading) }
    var metrics by remember { mutableStateOf<LoadState<EsgMetricDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId
            if (tab == 0) {
                lots = LoadState.Loading
                lots = runCatching { traceability(farmId) }.toLoadState()
            } else {
                metrics = LoadState.Loading
                metrics = runCatching { esg(farmId) }.toLoadState()
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
        Text(S.t("compliance.title"))
        SectionTabs(
            labels = listOf(S.t("tab.traceability"), S.t("tab.esg")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        if (tab == 0) {
            LoadedList(
                lots,
                format = { lot ->
                    listOfNotNull(lot.lotCode, lot.crop, lot.eventType, lot.summary, lot.occurredAt)
                        .joinToString(" · ")
                        .ifBlank { lot.id }
                },
                itemContent = { lot ->
                    val code = lot.lotCode
                    if (!code.isNullOrBlank()) {
                        TextButton(onClick = { onOpenLot(code) }) { Text(S.t("compliance.lot.open")) }
                    }
                },
            )
        } else {
            LoadedList(metrics) { m ->
                listOfNotNull(m.metric, m.value?.toString(), m.unit, m.score?.let { "score $it" }, m.periodLabel)
                    .joinToString(" · ")
                    .ifBlank { m.id }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
