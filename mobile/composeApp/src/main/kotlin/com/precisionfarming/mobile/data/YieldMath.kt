package com.precisionfarming.mobile.data

object YieldMath {
    fun weightedYieldTHa(rows: List<YieldRecordDto>): Double {
        val usable = rows.mapNotNull { row ->
            val yield = row.yieldTHa
            if (yield != null && yield.isFinite()) row else null
        }
        if (usable.isEmpty()) return 0.0
        val withArea = usable.filter { val area = it.areaHa; area != null && area > 0.0 }
        if (withArea.isNotEmpty()) {
            val num = withArea.sumOf { it.yieldTHa!! * it.areaHa!! }
            val den = withArea.sumOf { it.areaHa!! }
            return if (den == 0.0) 0.0 else num / den
        }
        return usable.sumOf { it.yieldTHa!! } / usable.size
    }

    fun yieldsForPlan(rows: List<YieldRecordDto>, plan: HarvestPlanDto): List<YieldRecordDto> =
        rows.filter { row -> row.planId == plan.id || (row.planId == null && row.fieldId == plan.fieldId) }

    fun yieldsForField(rows: List<YieldRecordDto>, fieldId: String): List<YieldRecordDto> =
        rows.filter { it.fieldId == fieldId }
}
