package com.precisionfarming.mobile.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Query-arg routes matching web `?selected=` — no path ids, so tab identity stays stable. */
object InspectNav {
    const val ARG_SELECTED = "selected"
    const val ARG_SEVERITY = "severity"

    const val HOME = "home"
    const val MAP = "mapa"
    const val OPS = "ops"
    const val ALERTS = "alertas"
    const val MORE = "mais"
    const val FARMS = "mais/farms"
    const val FIELDS = "mais/fields"
    const val MACHINES = "mais/machines"
    const val INVENTORY = "mais/inventory"
    const val HARVEST = "mais/harvest"
    /** Existing HarvestScreen tabs (plans / yield / logistics / storage). */
    const val HARVEST_DETAIL = "mais/harvest/detail"
    const val TOWER = "mais/tower"
    const val DECISIONS = "mais/decisions"
    const val INSIGHTS = "mais/insights"
    const val FINANCE = "mais/finance"
    const val SYNC = "mais/sync"
    const val SEASONS = "mais/seasons"
    const val MAINTENANCE = "mais/maintenance"
    const val AGRONOMY = "mais/agronomy"
    const val WEATHER = "mais/weather"
    const val IRRIGATION = "mais/irrigation"
    const val MARKET = "mais/market"
    const val COMPLIANCE = "mais/compliance"
    const val REPORTS = "mais/reports"
    const val INTEGRATIONS = "mais/integrations"
    const val SETTINGS = "mais/settings"
    const val ARG_LOT = "lotCode"
    const val COMPLIANCE_LOT = "mais/compliance/lot/{lotCode}"

    fun complianceLot(code: String): String = "mais/compliance/lot/${android.net.Uri.encode(code)}"

    /** Work-order execution screen; path id because it is a detail page, not a tab. */
    const val ARG_ID = "id"
    const val OPS_RUN = "ops/run/{$ARG_ID}"

    fun opsRun(id: String): String = "ops/run/${encodePath(id)}"

    fun pattern(base: String): String =
        if (base == ALERTS) "$base?severity={severity}&selected={selected}"
        else "$base?selected={selected}"

    fun href(base: String, selected: String? = null, severity: String? = null): String =
        if (base == ALERTS) {
            "$base?severity=${encodeQuery(severity)}&selected=${encodeQuery(selected)}"
        } else {
            "$base?selected=${encodeQuery(selected)}"
        }

    /** Decode a nav query/path argument (handles `%3A` composite decision ids). */
    fun decodeArg(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
        }.getOrDefault(raw).ifBlank { null }
    }

    fun baseOf(destinationRoute: String?): String = destinationRoute?.substringBefore("?") ?: ""

    fun tabSelected(destinationRoute: String?, tab: String): Boolean {
        val base = baseOf(destinationRoute)
        return when (tab) {
            MORE -> base == MORE || base.startsWith("mais/")
            OPS -> base == OPS || base.startsWith("ops/")
            else -> base == tab
        }
    }

    private fun encodeQuery(value: String?): String =
        URLEncoder.encode(value.orEmpty(), StandardCharsets.UTF_8.name())

    private fun encodePath(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

fun <T> List<T>.byId(id: String?, of: (T) -> String): T? {
    if (id.isNullOrBlank()) return null
    return firstOrNull { of(it) == id }
}
