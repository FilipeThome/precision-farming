package com.precisionfarming.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.login
import com.precisionfarming.mobile.data.offline.OfflineRuntime
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.LocalePill
import com.precisionfarming.mobile.ui.components.StatusTag
import com.precisionfarming.mobile.ui.components.TagTone
import com.precisionfarming.mobile.ui.theme.AgOsColors
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onOk: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !busy

    fun submit() {
        if (!canSubmit) return
        busy = true
        error = null
        scope.launch {
            runCatching { login(email.trim(), password) }
                .onSuccess { session ->
                    OfflineRuntime.bindSession(session.userId)
                    onOk()
                }
                .onFailure { error = it.message ?: S.t("common.error") }
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AgOsColors.g900, AgOsColors.g950)))
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(AgOsColors.t500, RoundedCornerShape(16.dp))
                        .semantics { contentDescription = S.t("app.name") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(S.t("login.mark"), style = MaterialTheme.typography.headlineSmall, color = AgOsColors.n0)
                }
                LocalePill(onDark = true)
            }
            Text(
                S.t("app.name"),
                style = MaterialTheme.typography.displayMedium,
                color = AgOsColors.n0,
                modifier = Modifier.padding(top = 18.dp).semantics { heading() },
            )
            Text(
                S.t("login.tagline"),
                style = MaterialTheme.typography.bodyLarge,
                color = AgOsColors.heroMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTag(S.t("login.offline"), tone = TagTone.OnDark, icon = Icons.Outlined.WifiOff)
                StatusTag(S.t("login.ownerData"), tone = TagTone.OnDark, icon = Icons.Outlined.VerifiedUser)
            }
        }

        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LoginField(
                label = S.t("login.email"),
                value = email,
                onChange = { email = it },
                leading = Icons.Outlined.Person,
                keyboard = KeyboardType.Email,
            )
            LoginField(
                label = S.t("login.password"),
                value = password,
                onChange = { password = it },
                leading = Icons.Outlined.Lock,
                keyboard = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    val label = if (showPassword) S.t("login.hidePassword") else S.t("login.showPassword")
                    IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = label,
                        )
                    }
                },
            )
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Assertive
                        contentDescription = it
                    },
                )
            }
            Button(
                onClick = { submit() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgOsColors.t500, contentColor = AgOsColors.n0),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = AgOsColors.n0, strokeWidth = 2.dp)
                } else {
                    Text(S.t("login.submit"), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
    keyboard: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AgOsColors.n800)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            leadingIcon = { Icon(leading, contentDescription = null, tint = AgOsColors.n600) },
            trailingIcon = trailing,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AgOsColors.t500,
                unfocusedBorderColor = AgOsColors.n300,
                focusedContainerColor = AgOsColors.n0,
                unfocusedContainerColor = AgOsColors.n0,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .semantics { contentDescription = label },
        )
    }
}
