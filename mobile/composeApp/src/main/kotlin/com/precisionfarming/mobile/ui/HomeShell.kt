package com.precisionfarming.mobile.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FarmFilter
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

private data class Tab(val route: String, val labelKey: String, val icon: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShell(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "nav.home", "H"),
        Tab("mapa", "nav.map", "M"),
        Tab("ops", "nav.ops", "O"),
        Tab("alertas", "nav.alerts", "A"),
        Tab("mais", "nav.more", "+"),
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
                        TextButton(onClick = {
                            if (farmLoadFailed) farmLoadEpoch++
                            farmMenu = true
                        }) { Text(farmLabel) }
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
        val back: () -> Unit = { nav.popBackStack() }
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { DashboardScreen() }
            composable("mapa") { MapScreen() }
            composable("ops") { OpsScreen() }
            composable("alertas") { AlertsScreen() }
            composable("mais") { MoreMenu(onOpen = { nav.navigate(it) }) }
            composable("mais/farms") { FarmsScreen(onBack = back) }
            composable("mais/fields") { FieldsScreen(onBack = back) }
            composable("mais/seasons") { SeasonsScreen(onBack = back) }
            composable("mais/machines") { MachinesScreen(onBack = back) }
            composable("mais/maintenance") { MaintenanceScreen(onBack = back) }
            composable("mais/agronomy") { AgronomyScreen(onBack = back) }
            composable("mais/weather") { WeatherScreen(onBack = back) }
            composable("mais/irrigation") { IrrigationScreen(onBack = back) }
            composable("mais/harvest") { HarvestScreen(onBack = back) }
            composable("mais/inventory") { InventoryScreen(onBack = back) }
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
            composable("mais/insights") { InsightsScreen(onBack = back) }
            composable("mais/reports") { ReportsScreen(onBack = back) }
            composable("mais/integrations") { IntegrationsScreen(onBack = back) }
            composable("mais/settings") { SettingsScreen(onBack = back, onLogout = onLogout) }
            composable("mais/sync") { SyncStatusScreen(onBack = back) }
        }
    }
}
