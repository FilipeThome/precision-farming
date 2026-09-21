package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.i18n.S

data class FormOption(val value: String, val label: String)

data class FormField(
    val name: String,
    val label: String,
    val required: Boolean = true,
    val options: List<FormOption>? = null,
)

@Composable
fun EntityFormSheet(
    title: String,
    fields: List<FormField>,
    values: Map<String, String>,
    onChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    pending: Boolean = false,
    error: String? = null,
    extra: @Composable () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fields.forEach { field ->
                    val options = field.options
                    if (options != null) {
                        Text(field.label)
                        options.forEach { option ->
                            val selected = values[field.name] == option.value
                            TextButton(onClick = { onChange(field.name, option.value) }) {
                                Text(if (selected) "✓ ${option.label}" else option.label)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = values[field.name].orEmpty(),
                            onValueChange = { onChange(field.name, it) },
                            label = { Text(field.label) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                extra()
                error?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !pending) {
                Text(if (pending) S.t("form.saving") else S.t("form.save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(S.t("form.cancel")) }
        },
    )
}
