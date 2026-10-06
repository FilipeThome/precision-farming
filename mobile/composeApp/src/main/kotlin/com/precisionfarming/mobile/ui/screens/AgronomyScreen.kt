package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.precisionfarming.mobile.data.MeDto
import com.precisionfarming.mobile.data.PrescriptionDto
import com.precisionfarming.mobile.data.RecommendationDto
import com.precisionfarming.mobile.data.ScoutingDto
import com.precisionfarming.mobile.data.SoilSampleDto
import com.precisionfarming.mobile.data.approvePrescription
import com.precisionfarming.mobile.data.canManageFarmOps
import com.precisionfarming.mobile.data.me
import com.precisionfarming.mobile.data.prescriptions
import com.precisionfarming.mobile.data.recommendations
import com.precisionfarming.mobile.data.scouting
import com.precisionfarming.mobile.data.soilSamples
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.LoadedList
import com.precisionfarming.mobile.ui.components.ScreenHeader
import com.precisionfarming.mobile.ui.components.SectionTabs
import com.precisionfarming.mobile.ui.components.toLoadState
import kotlinx.coroutines.launch

private enum class AgronomyTab { SCOUTING, SOIL, RECS, PRESCRIPTIONS }

@Composable
fun AgronomyScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var scout by remember { mutableStateOf<LoadState<ScoutingDto>>(LoadState.Loading) }
    var soil by remember { mutableStateOf<LoadState<SoilSampleDto>>(LoadState.Loading) }
    var recs by remember { mutableStateOf<LoadState<RecommendationDto>>(LoadState.Loading) }
    var rx by remember { mutableStateOf<LoadState<PrescriptionDto>>(LoadState.Loading) }
    var meDto by remember { mutableStateOf<MeDto?>(null) }
    var meFailed by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val tabs = AgronomyTab.entries
    val canApprove = canManageFarmOps(meDto?.role)
    fun reload() {
        scope.launch {
            val farmId = FarmFilter.farmId.value
            val meResult = runCatching { me() }
            meDto = meResult.getOrNull()
            meFailed = meResult.isFailure
            when (tabs[tab]) {
                AgronomyTab.SCOUTING -> {
                    scout = LoadState.Loading
                    scout = runCatching { scouting(farmId) }.toLoadState()
                }
                AgronomyTab.SOIL -> {
                    soil = LoadState.Loading
                    soil = runCatching { soilSamples(farmId) }.toLoadState()
                }
                AgronomyTab.RECS -> {
                    recs = LoadState.Loading
                    recs = runCatching { recommendations(farmId) }.toLoadState()
                }
                AgronomyTab.PRESCRIPTIONS -> {
                    rx = LoadState.Loading
                    rx = runCatching { prescriptions(farmId) }.toLoadState()
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
        ScreenHeader(S.t("agronomy.title"), onBack)
        SectionTabs(
            labels = listOf(
                S.t("tab.scouting"),
                S.t("tab.soil"),
                S.t("tab.recs"),
                S.t("tab.prescriptions"),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        msg?.let { Text(it) }
        when (tabs[tab]) {
            AgronomyTab.SCOUTING -> LoadedList(scout) { s ->
                listOfNotNull(s.pest, s.severity, s.status, s.notes).joinToString(" · ").ifBlank { s.id }
            }
            AgronomyTab.SOIL -> LoadedList(soil) { s ->
                buildString {
                    append(s.id.take(8))
                    s.ph?.let { append(" · pH $it") }
                    s.pPpm?.let { append(" · P $it") }
                    s.kPpm?.let { append(" · K $it") }
                    s.status?.let { append(" · $it") }
                }
            }
            AgronomyTab.RECS -> LoadedList(recs) { r ->
                listOfNotNull(r.kind, r.title, r.priority, r.summary, r.status)
                    .joinToString(" · ")
                    .ifBlank { r.id }
            }
            AgronomyTab.PRESCRIPTIONS -> LoadedList(
                rx,
                format = { p ->
                    listOfNotNull(p.product, "${p.dose} ${p.unit}", p.status)
                        .joinToString(" · ")
                        .ifBlank { p.id }
                },
                itemContent = { p ->
                    val draft = p.status.equals("DRAFT", ignoreCase = true)
                    if (draft && canApprove) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { approvePrescription(p.id) }
                                    .onSuccess { msg = S.t("decisions.approval.approved") }
                                    .onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("prescriptions.approve")) }
                    }
                    if (draft && meFailed) {
                        Text(S.t("auth.meLoadError"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (draft && !meFailed && meDto != null && !canApprove) {
                        Text(
                            S.t("decisions.approval.noPermission"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}
