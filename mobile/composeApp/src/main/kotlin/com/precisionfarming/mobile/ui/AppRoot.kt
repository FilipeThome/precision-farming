package com.precisionfarming.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.precisionfarming.mobile.data.completeOp
import com.precisionfarming.mobile.data.farms
import com.precisionfarming.mobile.data.insights
import com.precisionfarming.mobile.data.login
import com.precisionfarming.mobile.data.machines
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.data.startOp
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") { LoginScreen { nav.navigate("home") { popUpTo("login") { inclusive = true } } } }
        composable("home") { HomeShell() }
    }
}

@Composable
fun LoginScreen(onOk: () -> Unit) {
    var email by remember { mutableStateOf("manager@precisionfarming.demo") }
    var password by remember { mutableStateOf("Precision@123") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Precision Farming")
        OutlinedTextField(email, { email = it }, label = { Text("Email") })
        OutlinedTextField(password, { password = it }, label = { Text("Senha") })
        error?.let { Text(it) }
        Button(onClick = {
            scope.launch {
                runCatching { login(email, password) }
                    .onSuccess { onOk() }
                    .onFailure { error = it.message }
            }
        }) { Text("Entrar") }
    }
}

@Composable
fun HomeShell() {
    val nav = rememberNavController()
    val tabs = listOf("home", "mapa", "ops", "maquinas", "alertas", "mais")
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = false,
                        onClick = { nav.navigate(t) },
                        label = { Text(t) },
                        icon = { Text(t.take(1).uppercase()) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { ListScreen("Home") { farms().map { it.name } } }
            composable("mapa") {
                Column(Modifier.padding(16.dp)) {
                    Text("Mapa Google Satellite")
                    Text("Configure ANDROID_GOOGLE_MAPS_API_KEY. Sem chave o SDK não é inicializado — nenhum PNG estático.")
                }
            }
            composable("ops") { OpsScreen() }
            composable("maquinas") { ListScreen("Máquinas") { machines().map { "${it.name} · ${it.status}" } } }
            composable("alertas") { ListScreen("Alertas") { com.precisionfarming.mobile.data.alerts().map { it.title } } }
            composable("mais") { ListScreen("IA Insights") { insights().map { "${it.type} (${it.model})" } } }
        }
    }
}

@Composable
fun ListScreen(title: String, load: suspend () -> List<String>) {
    var items by remember { mutableStateOf(listOf("Carregando…")) }
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(title) {
        items = runCatching { load() }.getOrElse { listOf(it.message ?: "erro") }
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title)
        items.forEach { Text(it) }
        TextButton(onClick = { scope.launch { items = runCatching { load() }.getOrElse { listOf(it.message ?: "erro") } } }) {
            Text("Atualizar")
        }
    }
}

@Composable
fun OpsScreen() {
    var items by remember { mutableStateOf(listOf<com.precisionfarming.mobile.data.OperationDto>()) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        items = runCatching { operations() }.getOrDefault(emptyList())
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Operações")
        msg?.let { Text(it) }
        items.forEach { op ->
            Text("${op.type} · ${op.status}")
            TextButton(onClick = {
                scope.launch {
                    runCatching { startOp(op.id) }.onFailure { msg = it.message }
                    items = runCatching { operations() }.getOrDefault(items)
                }
            }) { Text("Iniciar") }
            TextButton(onClick = {
                scope.launch {
                    runCatching { completeOp(op.id) }.onFailure { msg = it.message }
                    items = runCatching { operations() }.getOrDefault(items)
                }
            }) { Text("Concluir") }
        }
    }
}
