package com.precisionfarming.mobile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Selected farm for list GETs. Null means all farms. */
object FarmFilter {
    var farmId by mutableStateOf<String?>(null)
}
