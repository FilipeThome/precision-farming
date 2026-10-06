package com.precisionfarming.mobile.ui

import androidx.navigation.NavController
import com.precisionfarming.mobile.data.FarmFilter

internal object AuthNav {
    const val LOGIN = "login"
    const val HOME = "home"

    fun startRoute(token: String?, userId: String?): String =
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) HOME else LOGIN

    fun shouldReplace(currentRoute: String?, target: String): Boolean =
        currentRoute != null && currentRoute != target
}

internal fun NavController.goToLogin() {
    if (!AuthNav.shouldReplace(currentDestination?.route, AuthNav.LOGIN)) return
    FarmFilter.farmId.value = null
    navigate(AuthNav.LOGIN) {
        popUpTo(AuthNav.HOME) { inclusive = true }
        launchSingleTop = true
    }
}

internal fun NavController.goToHome() {
    if (!AuthNav.shouldReplace(currentDestination?.route, AuthNav.HOME)) return
    navigate(AuthNav.HOME) {
        popUpTo(AuthNav.LOGIN) { inclusive = true }
        launchSingleTop = true
    }
}
