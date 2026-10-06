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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.MarketContractDto
import com.precisionfarming.mobile.data.MarketExposureDto
import com.precisionfarming.mobile.data.MarketQuoteDto
import com.precisionfarming.mobile.data.marketContracts
import com.precisionfarming.mobile.data.marketExposure
import com.precisionfarming.mobile.data.marketQuotes
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch
import com.precisionfarming.mobile.ui.components.ScreenHeader

private enum class MarketTab { QUOTES, CONTRACTS, EXPOSURE }

@Composable
fun MarketScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var quotes by remember { mutableStateOf<LoadState<MarketQuoteDto>>(LoadState.Loading) }
    var contracts by remember { mutableStateOf<LoadState<MarketContractDto>>(LoadState.Loading) }
    var exposure by remember { mutableStateOf<LoadState<MarketExposureDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    val tabs = MarketTab.entries
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId.value
            when (tabs[tab]) {
                MarketTab.QUOTES -> {
                    quotes = LoadState.Loading
                    quotes = runCatching { marketQuotes(farmId) }.toLoadState()
                }
                MarketTab.CONTRACTS -> {
                    contracts = LoadState.Loading
                    contracts = runCatching { marketContracts(farmId) }.toLoadState()
                }
                MarketTab.EXPOSURE -> {
                    exposure = LoadState.Loading
                    exposure = runCatching { marketExposure(farmId) }.toLoadState()
                }
            }
        }
    }
    LaunchedEffect(tab, LocalFarmId.current, LocalAppLocale.current) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(S.t("market.title"), onBack)
        SectionTabs(
            labels = listOf(S.t("tab.quotes"), S.t("tab.contracts"), S.t("tab.exposure")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        when (tabs[tab]) {
            MarketTab.QUOTES -> LoadedList(quotes) { q ->
                listOfNotNull(q.commodity, q.price?.toString(), q.currency, q.unit, q.market, q.quotedAt)
                    .joinToString(" · ")
                    .ifBlank { q.id }
            }
            MarketTab.CONTRACTS -> LoadedList(contracts) { c ->
                listOfNotNull(
                    c.commodity,
                    (c.volumeTons ?: c.volumeT)?.let { "${it}t" },
                    c.price?.toString(),
                    c.status,
                    c.counterparty,
                ).joinToString(" · ").ifBlank { c.id }
            }
            MarketTab.EXPOSURE -> LoadedList(exposure) { e ->
                listOfNotNull(
                    e.commodity,
                    e.openT?.let { "open $it" },
                    e.hedgedT?.let { "hedge $it" },
                    e.riskScore?.let { "${S.t("market.risk")} $it" },
                    e.asOf,
                ).joinToString(" · ").ifBlank { e.id }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
