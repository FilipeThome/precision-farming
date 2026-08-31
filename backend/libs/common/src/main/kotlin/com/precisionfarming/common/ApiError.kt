package com.precisionfarming.common

import java.time.Instant

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val code: String,
    val message: String,
    val correlationId: String? = null,
    val details: Map<String, Any?> = emptyMap(),
)
