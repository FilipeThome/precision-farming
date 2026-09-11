package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.i18n.S

/** Pause reason capture. Starts empty and requires non-blank input — no prefilled default. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseReasonSheet(
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    val valid = reason.isNotBlank()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                S.t("pause.title"),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(S.t("ops.pauseReason")) },
                placeholder = { Text(S.t("pause.placeholder")) },
                supportingText = if (!valid) ({ Text(S.t("pause.required")) }) else null,
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = S.t("ops.pauseReason") },
                shape = MaterialTheme.shapes.small,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                ) { Text(S.t("common.cancel")) }
                Button(
                    onClick = { onConfirm(reason.trim()) },
                    enabled = valid,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                ) { Text(S.t("pause.confirm")) }
            }
        }
    }
}
