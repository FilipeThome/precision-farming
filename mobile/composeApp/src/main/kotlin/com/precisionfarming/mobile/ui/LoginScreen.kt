package com.precisionfarming.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.login
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LocaleFlagButtons
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onOk: () -> Unit) {
    var email by remember { mutableStateOf(DemoLoginHints.EMAIL) }
    var password by remember { mutableStateOf(DemoLoginHints.PASSWORD) }
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
        OutlinedTextField(
            email,
            { email = it },
            label = { Text(S.t("login.email")) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            password,
            { password = it },
            label = { Text(S.t("login.password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
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
