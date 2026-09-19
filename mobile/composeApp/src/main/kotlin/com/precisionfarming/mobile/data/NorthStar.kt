package com.precisionfarming.mobile.data

data class Ratio(
    val numerator: Int,
    val denominator: Int,
    /** 0–100 rounded, or null when the denominator is 0 (render "—"). */
    val pct: Int?,
)

fun ratio(numerator: Int, denominator: Int): Ratio = Ratio(
    numerator = numerator,
    denominator = denominator,
    pct = if (denominator == 0) null else kotlin.math.round(numerator.toDouble() / denominator * 100).toInt(),
)

/** North Star: completed operations finished within their planned window / completed with both dates. */
fun withinWindowRatio(operations: List<OperationDto>): Ratio {
    val completed = operations.filter { it.status.equals("COMPLETED", ignoreCase = true) }
    val measurable = completed.filter { parseEpochMillis(it.actualEnd) != null && parseEpochMillis(it.plannedEnd) != null }
    val onTime = measurable.filter {
        (parseEpochMillis(it.actualEnd) as Long) <= (parseEpochMillis(it.plannedEnd) as Long)
    }
    return ratio(onTime.size, measurable.size)
}

/** Traceability proxy: operations with both a machine and an input item linked. */
fun traceabilityRatio(operations: List<OperationDto>): Ratio {
    val linked = operations.filter { !it.machineId.isNullOrBlank() && !it.itemId.isNullOrBlank() }
    return ratio(linked.size, operations.size)
}

fun openCriticalAlerts(alerts: List<AlertDto>): Int =
    alerts.count { it.status.equals("OPEN", ignoreCase = true) && it.severity.equals("CRITICAL", ignoreCase = true) }

/** Machines available for work (OPERATING or IDLE) / total. */
fun fleetAvailability(machines: List<MachineDto>): Ratio {
    val available = machines.count {
        it.status.equals("OPERATING", ignoreCase = true) || it.status.equals("IDLE", ignoreCase = true)
    }
    return ratio(available, machines.size)
}

/** End of a FAVORABLE window covering [now], or null when none does. */
fun favorableWindowUntil(windows: List<WeatherWindowDto>, now: Long = System.currentTimeMillis()): String? {
    val covering = windows
        .filter { it.rating.equals("FAVORABLE", ignoreCase = true) }
        .filter { w ->
            val start = parseEpochMillis(w.startAt)
            val end = parseEpochMillis(w.endAt)
            start != null && end != null && start <= now && now < end
        }
        .sortedByDescending { it.endAt.orEmpty() }
    return covering.firstOrNull()?.endAt
}
