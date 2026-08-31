package com.precisionfarming.common

data class PageResponse<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)
