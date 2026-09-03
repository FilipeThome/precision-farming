package com.precisionfarming.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.TokenStore
import com.precisionfarming.mobile.i18n.LocaleStore

@Composable
fun AppRoot() {
    LocaleStore.locale
    val restored = remember {
        TokenStore.read()?.also { token ->
            Session.accessToken = token
            Session.userId = TokenStore.readUserId()
        }
    }
    val canRestore = restored != null && !Session.userId.isNullOrBlank()
    if (restored != null && !canRestore) {
        TokenStore.clear()
        Session.clear()
    }
    val nav = rememberNavController()
    NavHost(nav, startDestination = if (canRestore) "home" else "login") {
        composable("login") {
            LoginScreen {
                nav.navigate("home") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("home") {
            HomeShell(
                onLogout = {
                    nav.navigate("login") { popUpTo("home") { inclusive = true } }
                },
            )
        }
    }
}
