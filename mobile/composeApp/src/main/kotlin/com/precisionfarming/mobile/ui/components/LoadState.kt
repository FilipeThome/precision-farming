package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.precisionfarming.mobile.i18n.S

sealed class LoadState<out T> {
    data object Loading : LoadState<Nothing>()
    data class Ok<T>(val items: List<T>) : LoadState<T>()
    data class Err(val message: String) : LoadState<Nothing>()
}

fun <T> Result<List<T>>.toLoadState(): LoadState<T> =
    fold(
        onSuccess = { LoadState.Ok(it) },
        onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
    )

@Composable
fun <T> LoadedList(
    state: LoadState<T>,
    itemContent: (@Composable (T) -> Unit)? = null,
    format: (T) -> String,
) {
    when (state) {
        is LoadState.Loading -> Text(S.t("common.loading"))
        is LoadState.Err -> Text("${S.t("common.error")}: ${state.message}")
        is LoadState.Ok -> {
            if (state.items.isEmpty()) {
                Text(S.t("common.empty"))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.items.forEach { item ->
                        Text(format(item))
                        itemContent?.invoke(item)
                    }
                }
            }
        }
    }
}
