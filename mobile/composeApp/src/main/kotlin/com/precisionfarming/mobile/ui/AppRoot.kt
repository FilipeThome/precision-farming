package com.precisionfarming.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.TokenStore
import com.precisionfarming.mobile.i18n.LocaleStore

@Composable
fun AppRoot() {
    LocaleStore.locale
    val startDestination = remember {
        val token = TokenStore.read()
        val userId = TokenStore.readUserId()
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            Session.set(token, userId)
            "home"
        } else {
            TokenStore.clear()
            Session.clear()
            "login"
        }
    }
    val signedIn by Session.signedIn.collectAsState()
    val nav = rememberNavController()
    NavHost(nav, startDestination = startDestination) {
        composable("login") {
            LoginScreen {
                nav.navigate("home") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("home") {
            HomeShell(
                onLogout = {
                    nav.navigate("login") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
    LaunchedEffect(signedIn) {
        val route = nav.currentBackStackEntry?.destination?.route
        if (!signedIn && route != null && route != "login") {
            FarmFilter.farmId = null
            nav.navigate("login") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
