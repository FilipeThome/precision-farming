package com.precisionfarming.mobile.ui.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PickedImage(val bytes: ByteArray, val mimeType: String, val fileName: String)

@Composable
fun rememberImagePickerLauncher(
    onPicked: (PickedImage) -> Unit,
    onError: (Throwable) -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("empty image")
                    if (bytes.isEmpty() || bytes.size > 5 * 1024 * 1024) error("invalid image")
                    if (mime !in setOf("image/jpeg", "image/png", "image/webp")) error("unsupported type")
                    PickedImage(bytes, mime, "machine.jpg")
                }
            }.onSuccess(onPicked).onFailure(onError)
        }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}
