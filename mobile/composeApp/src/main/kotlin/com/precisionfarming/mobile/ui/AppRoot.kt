package com.precisionfarming.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.TokenStore
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.i18n.LocaleStore

@Composable
fun AppRoot() {
    val locale by LocaleStore.locale.collectAsState()
    val farmId by FarmFilter.farmId.collectAsState()
    val startDestination = remember {
        val token = TokenStore.read()
        val userId = TokenStore.readUserId()
        val route = AuthNav.startRoute(token, userId)
        if (route == AuthNav.HOME) {
            Session.set(token, userId, TokenStore.readRole())
            if (!userId.isNullOrBlank()) OfflineRuntime.bindSession(userId)
        }
        else {
            TokenStore.clear()
            Session.clear()
        }
        route
    }
    val signedIn by Session.signedIn.collectAsState()
    val nav = rememberNavController()
    CompositionLocalProvider(LocalAppLocale provides locale, LocalFarmId provides farmId) {
        NavHost(nav, startDestination = startDestination) {
            composable(AuthNav.LOGIN) {
                LoginScreen { nav.goToHome() }
            }
            composable(AuthNav.HOME) {
                HomeShell(onLogout = { nav.goToLogin() })
            }
        }
        LaunchedEffect(signedIn) {
            if (!signedIn) nav.goToLogin()
        }
    }
}
