package com.precisionfarming.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.reportInventoryPdf
import com.precisionfarming.mobile.data.reportOperationsPdf
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.FileShare
import kotlinx.coroutines.launch
import com.precisionfarming.mobile.ui.components.ScreenHeader

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun share(kind: String, filename: String, load: suspend () -> ByteArray) {
        scope.launch {
            pending = kind
            error = null
            runCatching { FileShare.share(context, load(), filename, "application/pdf") }
                .onFailure { error = it.message ?: S.t("common.error") }
            pending = null
        }
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenHeader(S.t("reports.title"), onBack)
        error?.let { Text(it) }
        Button(
            enabled = pending == null,
            onClick = {
                share("operations", "operations.pdf") { reportOperationsPdf(FarmFilter.farmId.value) }
            },
        ) {
            Text(if (pending == "operations") S.t("reports.pending") else S.t("reports.operations"))
        }
        Button(
            enabled = pending == null,
            onClick = {
                share("inventory", "inventory.pdf") { reportInventoryPdf(FarmFilter.farmId.value) }
            },
        ) {
            Text(if (pending == "inventory") S.t("reports.pending") else S.t("reports.inventory"))
        }
    }
}
