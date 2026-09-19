package com.precisionfarming.mobile.data

data class TonsCoverage(val tons: Double, val withArea: Int, val total: Int)

/** Expected tons = Σ expectedTHa × field area, only for plans whose field has a known area. */
fun expectedTons(plans: List<HarvestPlanDto>, fields: List<FieldDto>): TonsCoverage {
    val areaById = fields.associate { it.id to it.areaHa }
    var tons = 0.0
    var withArea = 0
    for (plan in plans) {
        val area = plan.fieldId?.let { areaById[it] }
        if (plan.expectedTHa == null || area == null) continue
        tons += plan.expectedTHa * area
        withArea += 1
    }
    return TonsCoverage(tons = tons, withArea = withArea, total = plans.size)
}

/** Harvested tons = Σ yieldTHa × record area, only for records carrying an area. */
fun harvestedTons(records: List<YieldRecordDto>): TonsCoverage {
    var tons = 0.0
    var withArea = 0
    for (r in records) {
        if (r.yieldTHa == null || r.areaHa == null) continue
        tons += r.yieldTHa * r.areaHa
        withArea += 1
    }
    return TonsCoverage(tons = tons, withArea = withArea, total = records.size)
}

data class LoadsSummary(
    val queued: Int,
    val inTransit: Int,
    val delivered: Int,
    val total: Int,
    val tonsInTransit: Double,
)

private val IN_TRANSIT = setOf("DISPATCHED", "IN_TRANSIT", "IN_PROGRESS")
private val DELIVERED = setOf("DELIVERED", "COMPLETED", "RECEIVED")

fun loadsSummary(loads: List<LogisticsLoadDto>): LoadsSummary {
    var queued = 0
    var inTransit = 0
    var delivered = 0
    var tonsInTransit = 0.0
    for (load in loads) {
        val status = load.status.orEmpty().uppercase()
        when {
            status == "QUEUED" || status == "PLANNED" -> queued += 1
            status in IN_TRANSIT -> {
                inTransit += 1
                tonsInTransit += load.tons ?: 0.0
            }
            status in DELIVERED -> delivered += 1
        }
    }
    return LoadsSummary(
        queued = queued,
        inTransit = inTransit,
        delivered = delivered,
        total = loads.size,
        tonsInTransit = tonsInTransit,
    )
}

data class Occupancy(val usedT: Double, val capacityT: Double, val pct: Int?)

fun storageOccupancyTotal(units: List<StorageUnitDto>): Occupancy {
    var usedT = 0.0
    var capacityT = 0.0
    for (u in units) {
        usedT += u.usedT ?: 0.0
        capacityT += u.capacityT ?: 0.0
    }
    return Occupancy(
        usedT = usedT,
        capacityT = capacityT,
        pct = if (capacityT > 0) kotlin.math.round(usedT / capacityT * 100).toInt() else null,
    )
}

fun unitOccupancyPct(unit: StorageUnitDto): Int? {
    val cap = unit.capacityT ?: return null
    if (cap <= 0) return null
    return minOf(100, kotlin.math.round((unit.usedT ?: 0.0) / cap * 100).toInt())
}

data class QualityBucket(val quality: String, val count: Int, val tons: Double)

fun qualityBreakdown(lots: List<StorageLotDto>): List<QualityBucket> {
    val map = linkedMapOf<String, Pair<Int, Double>>()
    for (lot in lots) {
        val key = lot.quality?.takeIf { it.isNotBlank() } ?: "UNKNOWN"
        val cur = map[key] ?: (0 to 0.0)
        map[key] = (cur.first + 1) to (cur.second + (lot.tons ?: 0.0))
    }
    return map.entries
        .map { (quality, v) -> QualityBucket(quality, v.first, v.second) }
        .sortedByDescending { it.tons }
}

enum class FlowStageId { harvest, transport, storage, quality }

data class FlowStage(val id: FlowStageId, val state: ChainStepState, val count: Int)

private val ACTIVE_PLAN = setOf("IN_PROGRESS", "ACTIVE", "RUNNING")
private val DONE_PLAN = setOf("COMPLETED", "DONE", "CLOSED")

fun flowStages(
    plans: List<HarvestPlanDto>,
    loads: List<LogisticsLoadDto>,
    lots: List<StorageLotDto>,
): List<FlowStage> {
    val active = plans.count { ACTIVE_PLAN.contains(it.status.orEmpty().uppercase()) }
    val done = plans.count { DONE_PLAN.contains(it.status.orEmpty().uppercase()) }
    val harvestState = when {
        active > 0 -> ChainStepState.NOW
        plans.isNotEmpty() && done == plans.size -> ChainStepState.DONE
        else -> ChainStepState.PENDING
    }

    val summary = loadsSummary(loads)
    val transportState = when {
        summary.inTransit > 0 -> ChainStepState.NOW
        summary.delivered > 0 -> ChainStepState.DONE
        else -> ChainStepState.PENDING
    }

    val storageState = if (lots.isNotEmpty()) ChainStepState.DONE else ChainStepState.PENDING
    val graded = lots.count { !it.quality.isNullOrBlank() && it.quality != "UNKNOWN" }
    val qualityState = when {
        graded == 0 -> ChainStepState.PENDING
        graded == lots.size -> ChainStepState.DONE
        else -> ChainStepState.NOW
    }

    return listOf(
        FlowStage(FlowStageId.harvest, harvestState, active),
        FlowStage(FlowStageId.transport, transportState, summary.inTransit),
        FlowStage(FlowStageId.storage, storageState, lots.size),
        FlowStage(FlowStageId.quality, qualityState, graded),
    )
}

data class MarginByFieldRow(val fieldId: String, val margin: Double, val currency: String?)

/**
 * Filter + sort individual P&L rows (web MarginByField semantics), take [limit].
 * Rows without fieldId or margin are dropped; margins are not aggregated by field.
 */
fun marginRows(pnl: List<FinancePnlDto>, limit: Int = 8): List<MarginByFieldRow> =
    pnl
        .filter { !it.fieldId.isNullOrBlank() && it.margin != null }
        .sortedByDescending { it.margin ?: 0.0 }
        .take(limit)
        .map { MarginByFieldRow(checkNotNull(it.fieldId), checkNotNull(it.margin), it.currency) }
