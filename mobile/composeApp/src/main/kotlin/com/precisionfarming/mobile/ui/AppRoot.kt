package com.precisionfarming.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.precisionfarming.mobile.data.AlertDto
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.InsightDto
import com.precisionfarming.mobile.data.IrrigationAssetDto
import com.precisionfarming.mobile.data.IrrigationRecommendationDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.data.MaintenanceWorkOrderDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.RecommendationDto
import com.precisionfarming.mobile.data.ScoutingDto
import com.precisionfarming.mobile.data.SoilSampleDto
import com.precisionfarming.mobile.data.WeatherWindowDto
import com.precisionfarming.mobile.data.alerts
import com.precisionfarming.mobile.data.completeOp
import com.precisionfarming.mobile.data.completeWorkOrder
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.data.insights
import com.precisionfarming.mobile.data.irrigationAssets
import com.precisionfarming.mobile.data.irrigationRecommendations
import com.precisionfarming.mobile.data.login
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.maintenanceWorkOrders
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.data.recommendations
import com.precisionfarming.mobile.data.scouting
import com.precisionfarming.mobile.data.soilSamples
import com.precisionfarming.mobile.data.startOp
import com.precisionfarming.mobile.data.weatherWindows
import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.LocaleStore
import com.precisionfarming.mobile.i18n.S
import kotlinx.coroutines.launch

private sealed class LoadState<out T> {
    data object Loading : LoadState<Nothing>()
    data class Ok<T>(val items: List<T>) : LoadState<T>()
    data class Err(val message: String) : LoadState<Nothing>()
}

@Composable
fun AppRoot() {
    // Touch locale so root recomposes when language changes
    LocaleStore.locale
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") {
            LoginScreen { nav.navigate("home") { popUpTo("login") { inclusive = true } } }
        }
        composable("home") { HomeShell() }
    }
}

@Composable
fun LocaleFlagButtons() {
    val current = LocaleStore.locale
    Row {
        TextButton(
            onClick = { LocaleStore.setLocale(AppLocale.PT_BR) },
            enabled = current != AppLocale.PT_BR,
        ) {
            Text("🇧🇷 ${S.t("locale.br")}")
        }
        TextButton(
            onClick = { LocaleStore.setLocale(AppLocale.EN_US) },
            enabled = current != AppLocale.EN_US,
        ) {
            Text("🇺🇸 ${S.t("locale.us")}")
        }
    }
}

@Composable
fun LoginScreen(onOk: () -> Unit) {
    var email by remember { mutableStateOf("manager@precisionfarming.demo") }
    var password by remember { mutableStateOf("Precision@123") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LocaleFlagButtons()
        Text(S.t("app.name"))
        OutlinedTextField(email, { email = it }, label = { Text(S.t("login.email")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text(S.t("login.password")) }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it) }
        Button(onClick = {
            scope.launch {
                runCatching { login(email, password) }
                    .onSuccess { onOk() }
                    .onFailure { error = it.message ?: S.t("common.error") }
            }
        }) { Text(S.t("login.submit")) }
    }
}

private data class Tab(val route: String, val labelKey: String, val icon: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShell() {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "nav.home", "H"),
        Tab("mapa", "nav.map", "M"),
        Tab("ops", "nav.ops", "O"),
        Tab("maquinas", "nav.machines", "Q"),
        Tab("alertas", "nav.alerts", "A"),
        Tab("mais", "nav.more", "+"),
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(S.t("app.name")) },
                actions = { LocaleFlagButtons() },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route ||
                            (tab.route == "mais" && currentRoute?.startsWith("mais/") == true),
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(S.t(tab.labelKey)) },
                        icon = { Text(tab.icon) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                ApiListScreen(S.t("home.title")) {
                    farms().map { formatFarm(it) }
                }
            }
            composable("mapa") { MapScreen() }
            composable("ops") { OpsScreen() }
            composable("maquinas") {
                ApiListScreen(S.t("machines.title")) {
                    machines().map { formatMachine(it) }
                }
            }
            composable("alertas") {
                ApiListScreen(S.t("alerts.title")) {
                    alerts().map { formatAlert(it) }
                }
            }
            composable("mais") { MoreMenu(onOpen = { nav.navigate(it) }) }
            composable("mais/scouting") { ScoutingScreen(onBack = { nav.popBackStack() }) }
            composable("mais/weather") {
                ApiListScreen(S.t("weather.title"), onBack = { nav.popBackStack() }) {
                    weatherWindows().map { formatWindow(it) }
                }
            }
            composable("mais/irrigation") { IrrigationScreen(onBack = { nav.popBackStack() }) }
            composable("mais/maintenance") { MaintenanceScreen(onBack = { nav.popBackStack() }) }
            composable("mais/insights") {
                ApiListScreen(S.t("insights.title"), onBack = { nav.popBackStack() }) {
                    insights().map { formatInsight(it) }
                }
            }
        }
    }
}

@Composable
fun MoreMenu(onOpen: (String) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(S.t("more.title"))
        TextButton(onClick = { onOpen("mais/scouting") }) { Text(S.t("more.scouting")) }
        TextButton(onClick = { onOpen("mais/weather") }) { Text(S.t("more.weather")) }
        TextButton(onClick = { onOpen("mais/irrigation") }) { Text(S.t("more.irrigation")) }
        TextButton(onClick = { onOpen("mais/maintenance") }) { Text(S.t("more.maintenance")) }
        TextButton(onClick = { onOpen("mais/insights") }) { Text(S.t("more.insights")) }
    }
}

@Composable
fun MapScreen() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(S.t("map.title"))
        Text(S.t("map.noKey"))
    }
}

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
            state = runCatching { load() }
                .fold(
                    onSuccess = { LoadState.Ok(it) },
                    onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
                )
        }
    }
    LaunchedEffect(title, LocaleStore.locale) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text(S.t("common.back")) }
        }
        Text(title)
        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                if (s.items.isEmpty()) Text(S.t("common.empty"))
                else s.items.forEach { Text(it) }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}

@Composable
fun OpsScreen() {
    var state by remember { mutableStateOf<LoadState<OperationDto>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { operations() }
                .fold(
                    onSuccess = { LoadState.Ok(it) },
                    onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
                )
        }
    }
    LaunchedEffect(LocaleStore.locale) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(S.t("ops.title"))
        msg?.let { Text(it) }
        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                if (s.items.isEmpty()) Text(S.t("common.empty"))
                s.items.forEach { op ->
                    Text("${op.type} · ${op.status}")
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { startOp(op.id) }.onFailure { msg = it.message }
                            reload()
                        }
                    }) { Text(S.t("ops.start")) }
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { completeOp(op.id) }.onFailure { msg = it.message }
                            reload()
                        }
                    }) { Text(S.t("ops.complete")) }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}

@Composable
fun ScoutingScreen(onBack: () -> Unit) {
    var scout by remember { mutableStateOf<LoadState<ScoutingDto>>(LoadState.Loading) }
    var soil by remember { mutableStateOf<LoadState<SoilSampleDto>>(LoadState.Loading) }
    var recs by remember { mutableStateOf<LoadState<RecommendationDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            scout = LoadState.Loading
            soil = LoadState.Loading
            recs = LoadState.Loading
            scout = runCatching { scouting() }.fold({ LoadState.Ok(it) }, { LoadState.Err(it.message ?: S.t("common.error")) })
            soil = runCatching { soilSamples() }.fold({ LoadState.Ok(it) }, { LoadState.Err(it.message ?: S.t("common.error")) })
            recs = runCatching { recommendations() }.fold({ LoadState.Ok(it) }, { LoadState.Err(it.message ?: S.t("common.error")) })
        }
    }
    LaunchedEffect(Unit) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text(S.t("common.back")) }
        Text(S.t("scouting.title"))
        SectionList(S.t("scouting.observations"), scout) { formatScouting(it) }
        SectionList(S.t("scouting.soil"), soil) { formatSoil(it) }
        SectionList(S.t("scouting.recs"), recs) { formatRecommendation(it) }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}

@Composable
fun IrrigationScreen(onBack: () -> Unit) {
    var assets by remember { mutableStateOf<LoadState<IrrigationAssetDto>>(LoadState.Loading) }
    var recs by remember { mutableStateOf<LoadState<IrrigationRecommendationDto>>(LoadState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            assets = LoadState.Loading
            recs = LoadState.Loading
            assets = runCatching { irrigationAssets() }
                .fold({ LoadState.Ok(it) }, { LoadState.Err(it.message ?: S.t("common.error")) })
            recs = runCatching { irrigationRecommendations() }
                .fold({ LoadState.Ok(it) }, { LoadState.Err(it.message ?: S.t("common.error")) })
        }
    }
    LaunchedEffect(Unit) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text(S.t("common.back")) }
        Text(S.t("irrigation.title"))
        Text(S.t("irrigation.assets"))
        SectionList(null, assets) { formatIrrigationAsset(it) }
        Text(S.t("irrigation.recs"))
        SectionList(null, recs) { formatIrrigationRec(it) }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}

@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<MaintenanceWorkOrderDto>>(LoadState.Loading) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = LoadState.Loading
            state = runCatching { maintenanceWorkOrders() }
                .fold({ LoadState.Ok(it) }, { LoadState.Err(it.message ?: S.t("common.error")) })
        }
    }
    LaunchedEffect(Unit) { reload() }
    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text(S.t("common.back")) }
        Text(S.t("maintenance.title"))
        msg?.let { Text(it) }
        when (val s = state) {
            is LoadState.Loading -> Text(S.t("common.loading"))
            is LoadState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is LoadState.Ok -> {
                if (s.items.isEmpty()) Text(S.t("common.empty"))
                s.items.forEach { wo ->
                    Text(formatWorkOrder(wo))
                    if (wo.status?.equals("COMPLETED", ignoreCase = true) != true) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { completeWorkOrder(wo.id) }.onFailure { msg = it.message }
                                reload()
                            }
                        }) { Text(S.t("maintenance.complete")) }
                    }
                }
            }
        }
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
    }
}

@Composable
private fun <T> SectionList(label: String?, state: LoadState<T>, format: (T) -> String) {
    if (label != null) Text(label)
    when (state) {
        is LoadState.Loading -> Text(S.t("common.loading"))
        is LoadState.Err -> Text("${S.t("common.error")}: ${state.message}")
        is LoadState.Ok -> {
            if (state.items.isEmpty()) Text(S.t("common.empty"))
            else state.items.forEach { Text(format(it)) }
        }
    }
}

private fun formatFarm(f: FarmDto) = "${f.name} · ${f.location}"
private fun formatMachine(m: MachineDto) = "${m.name} · ${m.status}"
private fun formatAlert(a: AlertDto) = "${a.title} · ${a.severity}"
private fun formatInsight(i: InsightDto) = "${i.type} (${i.model}) · ${i.score}"
private fun formatScouting(s: ScoutingDto) =
    listOfNotNull(s.pest, s.severity, s.status, s.notes).joinToString(" · ").ifBlank { s.id }
private fun formatSoil(s: SoilSampleDto) =
    buildString {
        append(s.id.take(8))
        s.ph?.let { append(" · pH $it") }
        s.pPpm?.let { append(" · P $it") }
        s.kPpm?.let { append(" · K $it") }
        s.status?.let { append(" · $it") }
    }
private fun formatRecommendation(r: RecommendationDto) =
    listOfNotNull(r.kind, r.title, r.priority, r.summary, r.status).joinToString(" · ").ifBlank { r.id }
private fun formatWindow(w: WeatherWindowDto) =
    listOfNotNull(w.windowType, w.rating, w.startAt, w.notes).joinToString(" · ").ifBlank { w.id }
private fun formatIrrigationAsset(a: IrrigationAssetDto) =
    buildString {
        append(listOfNotNull(a.name, a.type, a.status).joinToString(" · ").ifBlank { a.id })
        a.capacityMmH?.let { append(" · ${it}mm/h") }
    }
private fun formatIrrigationRec(r: IrrigationRecommendationDto) =
    buildString {
        append(r.id.take(8))
        r.recommendedMm?.let { append(" · ${it}mm") }
        r.status?.let { append(" · $it") }
        r.reason?.let { append(" · $it") }
    }
private fun formatWorkOrder(w: MaintenanceWorkOrderDto) =
    listOfNotNull(w.title, w.priority, w.status, w.createdAt).joinToString(" · ").ifBlank { w.id }
