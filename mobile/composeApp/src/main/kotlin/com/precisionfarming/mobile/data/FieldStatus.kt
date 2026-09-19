package com.precisionfarming.mobile.data

enum class FieldState {
    progress, blocked, planned, done, stale, none
}

private val RANK = mapOf(
    FieldState.progress to 0,
    FieldState.blocked to 1,
    FieldState.planned to 2,
    FieldState.done to 3,
    FieldState.stale to 4,
    FieldState.none to 5,
)

private const val DEFAULT_STALE_AFTER_MS = 24L * 60 * 60 * 1000

private fun stateOf(status: String): FieldState? = when (status.uppercase()) {
    "IN_PROGRESS" -> FieldState.progress
    "PAUSED" -> FieldState.blocked
    "PLANNED" -> FieldState.planned
    "COMPLETED" -> FieldState.done
    else -> null
}

/**
 * Field → operation state used for Tower / Map status lists.
 * Priority: in progress > paused > planned > completed.
 * Fields without operations are [FieldState.none].
 * Fields whose only terminal activity is older than [staleAfterMs] become [FieldState.stale].
 */
fun fieldStates(
    fields: List<FieldDto>,
    operations: List<OperationDto>,
    now: Long = System.currentTimeMillis(),
    staleAfterMs: Long = DEFAULT_STALE_AFTER_MS,
): Map<String, FieldState> {
    val known = fields.map { it.id }.toSet()
    val result = linkedMapOf<String, FieldState>()
    for (op in operations) {
        val fieldId = op.fieldId ?: continue
        if (fieldId !in known) continue
        val next = stateOf(op.status) ?: continue
        val current = result[fieldId]
        if (current == null || RANK.getValue(next) < RANK.getValue(current)) {
            result[fieldId] = next
        }
    }
    for (field in fields) {
        val current = result[field.id]
        if (current == null) {
            result[field.id] = FieldState.none
            continue
        }
        if (current == FieldState.done && isStaleDone(field.id, operations, now, staleAfterMs)) {
            result[field.id] = FieldState.stale
        }
    }
    return result
}

private fun isStaleDone(
    fieldId: String,
    operations: List<OperationDto>,
    now: Long,
    staleAfterMs: Long,
): Boolean {
    val ends = operations
        .filter { it.fieldId == fieldId && it.status.equals("COMPLETED", ignoreCase = true) }
        .mapNotNull { parseEpochMillis(it.actualEnd) ?: parseEpochMillis(it.plannedEnd) }
    val last = ends.maxOrNull() ?: return false
    return now - last > staleAfterMs
}

fun countByState(states: Map<String, FieldState>): Map<FieldState, Int> {
    val counts = FieldState.entries.associateWith { 0 }.toMutableMap()
    for (state in states.values) counts[state] = counts.getValue(state) + 1
    return counts
}
