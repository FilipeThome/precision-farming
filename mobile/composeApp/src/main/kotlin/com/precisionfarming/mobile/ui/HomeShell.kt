package com.precisionfarming.mobile.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.InspectNav
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LocaleFlagButtons
import com.precisionfarming.mobile.ui.screens.AlertsScreen
import com.precisionfarming.mobile.ui.screens.AgronomyScreen
import com.precisionfarming.mobile.ui.screens.ComplianceScreen
import com.precisionfarming.mobile.ui.screens.DashboardScreen
import com.precisionfarming.mobile.ui.screens.FarmsScreen
import com.precisionfarming.mobile.ui.screens.FieldsScreen
import com.precisionfarming.mobile.ui.screens.FinanceScreen
import com.precisionfarming.mobile.ui.screens.HarvestScreen
import com.precisionfarming.mobile.ui.screens.InsightsScreen
import com.precisionfarming.mobile.ui.screens.IntegrationsScreen
import com.precisionfarming.mobile.ui.screens.InventoryScreen
import com.precisionfarming.mobile.ui.screens.IrrigationScreen
import com.precisionfarming.mobile.ui.screens.MachinesScreen
import com.precisionfarming.mobile.ui.screens.MaintenanceScreen
import com.precisionfarming.mobile.ui.screens.MapScreen
import com.precisionfarming.mobile.ui.screens.MarketScreen
import com.precisionfarming.mobile.ui.screens.MoreMenu
import com.precisionfarming.mobile.ui.screens.OpsScreen
import com.precisionfarming.mobile.ui.screens.ReportsScreen
import com.precisionfarming.mobile.ui.screens.SeasonsScreen
import com.precisionfarming.mobile.ui.screens.SettingsScreen
import com.precisionfarming.mobile.ui.screens.SyncStatusScreen
import com.precisionfarming.mobile.ui.screens.TraceabilityLotScreen
import com.precisionfarming.mobile.ui.screens.WeatherScreen

private data class Tab(val route: String, val labelKey: String, val icon: ImageVector)

private val selectedArg = navArgument(InspectNav.ARG_SELECTED) {
    type = NavType.StringType
    defaultValue = ""
}
private val severityArg = navArgument(InspectNav.ARG_SEVERITY) {
    type = NavType.StringType
    defaultValue = ""
}

private fun NavController.openInspect(href: String) {
    val destBase = href.substringBefore("?")
    val currentBase = InspectNav.baseOf(currentDestination?.route)
    val hasPayload = href.substringAfter("?", "").split("&").any { part ->
        part.substringAfter("=", "").isNotBlank()
    }
    navigate(href) {
        launchSingleTop = true
        if (currentBase != destBase) {
            restoreState = !hasPayload
            popUpTo(graph.findStartDestination().id) { saveState = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShell(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab(InspectNav.HOME, "nav.home", Icons.Filled.Home),
        Tab(InspectNav.MAP, "nav.map", Icons.Filled.Place),
        Tab(InspectNav.OPS, "nav.ops", Icons.AutoMirrored.Filled.List),
        Tab(InspectNav.ALERTS, "nav.alerts", Icons.Filled.Notifications),
        Tab(InspectNav.MORE, "nav.more", Icons.Filled.Menu),
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var farmMenu by remember { mutableStateOf(false) }
    var farmList by remember { mutableStateOf<List<FarmDto>>(emptyList()) }
    var farmLoadFailed by remember { mutableStateOf(false) }
    var farmLoadEpoch by remember { mutableIntStateOf(0) }
    LaunchedEffect(farmLoadEpoch) {
        val result = runCatching { farms() }
        result.onSuccess {
            farmList = it
            farmLoadFailed = false
        }.onFailure {
            farmLoadFailed = true
        }
    }
    val farmLabel = farmList.firstOrNull { it.id == FarmFilter.farmId }?.name
        ?: S.t("farm.filter.all")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(S.t("app.name")) },
                actions = {
                    Box {
                        TextButton(
                            onClick = {
                                if (farmLoadFailed) farmLoadEpoch++
                                farmMenu = true
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = farmLabel },
                        ) { Text(farmLabel) }
                        DropdownMenu(expanded = farmMenu, onDismissRequest = { farmMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(S.t("farm.filter.all")) },
                                onClick = {
                                    FarmFilter.farmId = null
                                    farmMenu = false
                                },
                            )
                            farmList.forEach { farm ->
                                DropdownMenuItem(
                                    text = { Text(farm.name) },
                                    onClick = {
                                        FarmFilter.farmId = farm.id
                                        farmMenu = false
                                    },
                                )
                            }
                            if (farmLoadFailed) {
                                DropdownMenuItem(
                                    text = { Text(S.t("common.refresh")) },
                                    onClick = {
                                        farmLoadEpoch++
                                        farmMenu = false
                                    },
                                )
                            }
                        }
                    }
                    LocaleFlagButtons()
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val label = S.t(tab.labelKey)
                    NavigationBarItem(
                        selected = InspectNav.tabSelected(currentRoute, tab.route),
                        onClick = {
                            nav.navigate(if (tab.route == InspectNav.MORE) InspectNav.MORE else InspectNav.href(tab.route)) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(label) },
                        icon = { Icon(tab.icon, contentDescription = label) },
                    )
                }
            }
        },
    ) { padding ->
        val back: () -> Unit = { nav.popBackStack() }
        fun selectedOf(entry: androidx.navigation.NavBackStackEntry) =
            entry.arguments?.getString(InspectNav.ARG_SELECTED).orEmpty().ifBlank { null }
        fun severityOf(entry: androidx.navigation.NavBackStackEntry) =
            entry.arguments?.getString(InspectNav.ARG_SEVERITY).orEmpty().ifBlank { null }
        fun setSelected(base: String, id: String, severity: String? = null) {
            nav.openInspect(InspectNav.href(base, selected = id, severity = severity))
        }
        fun clearSelected(base: String, severity: String? = null) {
            nav.openInspect(InspectNav.href(base, selected = null, severity = severity))
        }
        NavHost(nav, startDestination = InspectNav.HOME, modifier = Modifier.padding(padding)) {
            composable(InspectNav.HOME) { DashboardScreen(onOpen = { nav.openInspect(it) }) }
            composable(InspectNav.MAP) { MapScreen() }
            composable(InspectNav.pattern(InspectNav.OPS), arguments = listOf(selectedArg)) { entry ->
                OpsScreen(
                    selectedId = selectedOf(entry),
                    onSelect = { setSelected(InspectNav.OPS, it) },
                    onClearSelected = { clearSelected(InspectNav.OPS) },
                )
            }
            composable(
                InspectNav.pattern(InspectNav.ALERTS),
                arguments = listOf(severityArg, selectedArg),
            ) { entry ->
                AlertsScreen(
                    selectedId = selectedOf(entry),
                    severity = severityOf(entry),
                    onSelect = { setSelected(InspectNav.ALERTS, it, severityOf(entry)) },
                    onSeverity = { nav.openInspect(InspectNav.href(InspectNav.ALERTS, selected = selectedOf(entry), severity = it)) },
                    onClearSelected = { clearSelected(InspectNav.ALERTS, severityOf(entry)) },
                    onOpen = { nav.openInspect(it) },
                )
            }
            composable(InspectNav.MORE) {
                MoreMenu(onOpen = { route ->
                    val href = when (route) {
                        InspectNav.FARMS, InspectNav.FIELDS, InspectNav.MACHINES,
                        InspectNav.INVENTORY, InspectNav.HARVEST, InspectNav.INSIGHTS,
                        -> InspectNav.href(route)
                        else -> route
                    }
                    nav.navigate(href)
                })
            }
            composable(InspectNav.pattern(InspectNav.FARMS), arguments = listOf(selectedArg)) { entry ->
                FarmsScreen(selectedOf(entry), { setSelected(InspectNav.FARMS, it) }, { clearSelected(InspectNav.FARMS) }, back)
            }
            composable(InspectNav.pattern(InspectNav.FIELDS), arguments = listOf(selectedArg)) { entry ->
                FieldsScreen(selectedOf(entry), { setSelected(InspectNav.FIELDS, it) }, { clearSelected(InspectNav.FIELDS) }, back)
            }
            composable("mais/seasons") { SeasonsScreen(onBack = back) }
            composable(InspectNav.pattern(InspectNav.MACHINES), arguments = listOf(selectedArg)) { entry ->
                MachinesScreen(selectedOf(entry), { setSelected(InspectNav.MACHINES, it) }, { clearSelected(InspectNav.MACHINES) }, back)
            }
            composable("mais/maintenance") { MaintenanceScreen(onBack = back) }
            composable("mais/agronomy") { AgronomyScreen(onBack = back) }
            composable("mais/weather") { WeatherScreen(onBack = back) }
            composable("mais/irrigation") { IrrigationScreen(onBack = back) }
            composable(InspectNav.pattern(InspectNav.HARVEST), arguments = listOf(selectedArg)) { entry ->
                HarvestScreen(selectedOf(entry), { setSelected(InspectNav.HARVEST, it) }, { clearSelected(InspectNav.HARVEST) }, back)
            }
            composable(InspectNav.pattern(InspectNav.INVENTORY), arguments = listOf(selectedArg)) { entry ->
                InventoryScreen(selectedOf(entry), { setSelected(InspectNav.INVENTORY, it) }, { clearSelected(InspectNav.INVENTORY) }, back)
            }
            composable("mais/finance") { FinanceScreen(onBack = back) }
            composable("mais/market") { MarketScreen(onBack = back) }
            composable("mais/compliance") {
                ComplianceScreen(
                    onBack = back,
                    onOpenLot = { code ->
                        nav.navigate("mais/compliance/lot/${Uri.encode(code)}")
                    },
                )
            }
            composable(
                "mais/compliance/lot/{lotCode}",
                arguments = listOf(navArgument("lotCode") { type = NavType.StringType }),
            ) { entry ->
                val lotCode = Uri.decode(entry.arguments?.getString("lotCode").orEmpty())
                TraceabilityLotScreen(lotCode = lotCode, onBack = back)
            }
            composable(InspectNav.pattern(InspectNav.INSIGHTS), arguments = listOf(selectedArg)) { entry ->
                InsightsScreen(selectedOf(entry), { setSelected(InspectNav.INSIGHTS, it) }, { clearSelected(InspectNav.INSIGHTS) }, back)
            }
            composable("mais/reports") { ReportsScreen(onBack = back) }
            composable("mais/integrations") { IntegrationsScreen(onBack = back) }
            composable("mais/settings") { SettingsScreen(onBack = back, onLogout = onLogout) }
            composable("mais/sync") { SyncStatusScreen(onBack = back) }
        }
    }
}
