package com.precisionfarming.reporting.application

data class ReportCatalogItemDto(
    val kind: String,
    val title: String,
    val format: String,
    val filename: String,
    val path: String,
)
