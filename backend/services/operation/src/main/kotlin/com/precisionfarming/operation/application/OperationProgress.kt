package com.precisionfarming.operation.application

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class MachineWorkDay(val day: String, val areaHa: BigDecimal)
data class MachineWorkInput(val itemId: UUID, val quantity: BigDecimal)
data class MachineWorkSummaryDto(
    val areaHa: BigDecimal,
    val days: List<MachineWorkDay>,
    val inputs: List<MachineWorkInput>,
)

data class WorkSample(
    val status: String,
    val areaHa: BigDecimal?,
    val itemId: UUID?,
    val itemQuantity: BigDecimal?,
    val actualStart: Instant?,
    val plannedStart: Instant?,
)

object OperationProgress {
    private val DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)

    fun weight(status: String): Double = when (status) {
        "COMPLETED" -> 1.0
        "IN_PROGRESS", "STARTING", "COMPLETING" -> 0.5
        "PAUSED" -> 0.25
        else -> 0.0
    }

    fun summarize(ops: List<WorkSample>): MachineWorkSummaryDto {
        var area = BigDecimal.ZERO
        val dayBuckets = linkedMapOf<String, BigDecimal>()
        val inputBuckets = linkedMapOf<UUID, BigDecimal>()
        for (op in ops) {
            val w = BigDecimal.valueOf(weight(op.status))
            val whenAt = op.actualStart ?: op.plannedStart
            val covered = (op.areaHa ?: BigDecimal.ZERO).multiply(w).setScale(4, RoundingMode.HALF_UP)
            area = area.add(covered)
            if (whenAt != null && covered > BigDecimal.ZERO) {
                val day = DAY.format(whenAt)
                dayBuckets[day] = (dayBuckets[day] ?: BigDecimal.ZERO).add(covered)
            }
            val itemId = op.itemId
            val qty = op.itemQuantity
            if (itemId != null && qty != null) {
                val used = qty.multiply(w).setScale(3, RoundingMode.HALF_UP)
                if (used > BigDecimal.ZERO) {
                    inputBuckets[itemId] = (inputBuckets[itemId] ?: BigDecimal.ZERO).add(used)
                }
            }
        }
        return MachineWorkSummaryDto(
            areaHa = area.setScale(1, RoundingMode.HALF_UP),
            days = dayBuckets.entries.sortedBy { it.key }.map { MachineWorkDay(it.key, it.value.setScale(1, RoundingMode.HALF_UP)) },
            inputs = inputBuckets.entries.map { MachineWorkInput(it.key, it.value.setScale(1, RoundingMode.HALF_UP)) },
        )
    }
}
