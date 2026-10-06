package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.MeDto
import com.precisionfarming.mobile.data.Session
import com.precisionfarming.mobile.data.TokenStore
import com.precisionfarming.mobile.data.me
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LocaleFlagButtons
import kotlinx.coroutines.launch
import com.precisionfarming.mobile.ui.components.ScreenHeader

private sealed class MeState {
    data object Loading : MeState()
    data class Ok(val me: MeDto) : MeState()
    data class Err(val message: String) : MeState()
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    var state by remember { mutableStateOf<MeState>(MeState.Loading) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            state = MeState.Loading
            state = runCatching { me() }.fold(
                onSuccess = { MeState.Ok(it) },
                onFailure = { MeState.Err(it.message ?: S.t("common.error")) },
            )
        }
    }
    LaunchedEffect(LocalAppLocale.current) { reload() }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenHeader(S.t("settings.title"), onBack)
        when (val s = state) {
            is MeState.Loading -> Text(S.t("common.loading"))
            is MeState.Err -> Text("${S.t("common.error")}: ${s.message}")
            is MeState.Ok -> {
                Text("${S.t("settings.name")}: ${s.me.name}")
                Text("${S.t("settings.email")}: ${s.me.email}")
                Text("${S.t("settings.role")}: ${s.me.role}")
                Text("${S.t("settings.id")}: ${s.me.id}")
            }
        }
        LocaleFlagButtons()
        TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) }
        Button(onClick = {
            OfflineRuntime.onLogout()
            TokenStore.clear()
            Session.clear()
            FarmFilter.farmId.value = null
            onLogout()
        }) { Text(S.t("settings.logout")) }
    }
}
