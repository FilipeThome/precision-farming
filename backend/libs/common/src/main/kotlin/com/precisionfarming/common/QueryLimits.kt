package com.precisionfarming.common

object QueryLimits {
    const val MAX_LIST = 500
    const val MAX_SYNC_COMMANDS = 100
    const val MAX_TELEMETRY_DAYS = 31L
    const val VT_PARALLELISM = 16
}

fun <T> List<T>.capped(max: Int = QueryLimits.MAX_LIST): List<T> =
    if (size <= max) this else take(max)

fun <T> List<T>.cappedNewest(max: Int = QueryLimits.MAX_LIST): List<T> =
    if (size <= max) this else takeLast(max)
