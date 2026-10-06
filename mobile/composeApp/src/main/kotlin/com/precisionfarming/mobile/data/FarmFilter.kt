package com.precisionfarming.mobile.data

import kotlinx.coroutines.flow.MutableStateFlow

/** Selected farm for list GETs. Null means all farms. */
object FarmFilter {
    val farmId = MutableStateFlow<String?>(null)

    fun apply(id: String?) {
        if (!id.isNullOrBlank()) farmId.value = id
    }
}
