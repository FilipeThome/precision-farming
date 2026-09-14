package com.precisionfarming.mobile.data

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
    const val INSIGHTS = "mais/insights"
    const val FINANCE = "mais/finance"
    const val SYNC = "mais/sync"

    /** Work-order execution screen; path id because it is a detail page, not a tab. */
    const val ARG_ID = "id"
    const val OPS_RUN = "ops/run/{$ARG_ID}"

    fun opsRun(id: String): String = "ops/run/${java.net.URLEncoder.encode(id, "UTF-8")}"

    fun pattern(base: String): String =
        if (base == ALERTS) "$base?severity={severity}&selected={selected}"
        else "$base?selected={selected}"

    fun href(base: String, selected: String? = null, severity: String? = null): String =
        if (base == ALERTS) {
            "$base?severity=${severity.orEmpty()}&selected=${selected.orEmpty()}"
        } else {
            "$base?selected=${selected.orEmpty()}"
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
}

fun <T> List<T>.byId(id: String?, of: (T) -> String): T? {
    if (id.isNullOrBlank()) return null
    return firstOrNull { of(it) == id }
}
