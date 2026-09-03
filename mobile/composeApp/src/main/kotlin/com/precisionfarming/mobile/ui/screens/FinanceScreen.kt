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
import com.precisionfarming.mobile.data.FinanceBudgetDto
import com.precisionfarming.mobile.data.FinanceCashflowDto
import com.precisionfarming.mobile.data.FinanceCostDto
import com.precisionfarming.mobile.data.FinancePnlDto
import com.precisionfarming.mobile.data.financeBudget
import com.precisionfarming.mobile.data.financeCashflow
import com.precisionfarming.mobile.data.financeCosts
import com.precisionfarming.mobile.data.financePnl
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

private enum class FinanceTab { COSTS, PNL, BUDGET, CASHFLOW }

@Composable
fun FinanceScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var costs by remember { mutableStateOf<LoadState<FinanceCostDto>>(LoadState.Loading) }
    var pnl by remember { mutableStateOf<LoadState<FinancePnlDto>>(LoadState.Loading) }
    var budget by remember { mutableStateOf<LoadState<FinanceBudgetDto>>(LoadState.Loading) }
    var cash by remember { mutableStateOf<LoadState<FinanceCashflowDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    val tabs = FinanceTab.entries
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId
            when (tabs[tab]) {
                FinanceTab.COSTS -> {
                    costs = LoadState.Loading
                    costs = runCatching { financeCosts(farmId) }.toLoadState()
                }
                FinanceTab.PNL -> {
                    pnl = LoadState.Loading
                    pnl = runCatching { financePnl(farmId) }.toLoadState()
                }
                FinanceTab.BUDGET -> {
                    budget = LoadState.Loading
                    budget = runCatching { financeBudget(farmId) }.toLoadState()
                }
                FinanceTab.CASHFLOW -> {
                    cash = LoadState.Loading
                    cash = runCatching { financeCashflow(farmId) }.toLoadState()
                }
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
        Text(S.t("finance.title"))
        SectionTabs(
            labels = listOf(S.t("tab.costs"), S.t("tab.pnl"), S.t("tab.budget"), S.t("tab.cashflow")),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        when (tabs[tab]) {
            FinanceTab.COSTS -> LoadedList(costs) { c ->
                listOfNotNull(c.category, c.description, c.amount?.toString(), c.currency, c.occurredAt)
                    .joinToString(" · ")
                    .ifBlank { c.id }
            }
            FinanceTab.PNL -> LoadedList(pnl) { p ->
                listOfNotNull(
                    p.period,
                    p.revenue?.let { "${S.t("dashboard.pnl.revenue")} $it" },
                    p.cost?.let { "${S.t("dashboard.pnl.cost")} $it" },
                    p.margin?.let { "${S.t("dashboard.pnl.margin")} $it" },
                    p.currency,
                ).joinToString(" · ").ifBlank { p.id ?: "—" }
            }
            FinanceTab.BUDGET -> LoadedList(budget) { b ->
                listOfNotNull(
                    b.category,
                    b.seasonLabel,
                    b.planned?.let { "plan $it" },
                    b.actual?.let { "real $it" },
                    b.currency,
                ).joinToString(" · ").ifBlank { b.id }
            }
            FinanceTab.CASHFLOW -> LoadedList(cash) { c ->
                listOfNotNull(c.label, c.direction, c.amount?.toString(), c.dueAt)
                    .joinToString(" · ")
                    .ifBlank { c.id }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
