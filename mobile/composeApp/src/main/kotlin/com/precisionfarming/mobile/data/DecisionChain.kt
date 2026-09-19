package com.precisionfarming.mobile.data

enum class ChainStepId { SIGNAL, CONTEXT, RECOMMENDATION, APPROVAL, ORDER, EXECUTION }

enum class ChainStepState { DONE, NOW, PENDING, BLOCKED }

data class DerivedChainStep(
    val id: ChainStepId,
    val state: ChainStepState,
    /** True when the state comes from a heuristic (e.g. linked operation by field + time). */
    val inferred: Boolean = false,
)

val CHAIN_STEP_IDS: List<ChainStepId> = listOf(
    ChainStepId.SIGNAL,
    ChainStepId.CONTEXT,
    ChainStepId.RECOMMENDATION,
    ChainStepId.APPROVAL,
    ChainStepId.ORDER,
    ChainStepId.EXECUTION,
)

/**
 * Heuristic: an operation on the same field that started (or is planned to start) after the approval.
 * Labeled as inferred in the UI — the backend has no decision→operation link.
 */
fun linkedOperation(item: DecisionItem, operations: List<OperationDto>): OperationDto? {
    val fieldId = item.fieldId ?: return null
    val approvedAt = item.approvedAt ?: return null
    val approved = parseEpochMillis(approvedAt) ?: return null
    val candidates = operations
        .filter { it.fieldId == fieldId }
        .filter { op ->
            val start = op.actualStart ?: op.plannedStart ?: return@filter false
            val at = parseEpochMillis(start) ?: return@filter false
            at >= approved
        }
        .sortedBy { it.actualStart ?: it.plannedStart ?: "" }
    // Ambiguous matches are not linked — two orders on the same field must not share a chain.
    return candidates.singleOrNull()
}

/** Derives the six renderable chain steps for a decision from real data only. */
fun chainSteps(item: DecisionItem, operations: List<OperationDto> = emptyList()): List<DerivedChainStep> {
    val signal = if (item.createdAt != null) ChainStepState.DONE else ChainStepState.PENDING
    val hasContext = item.fieldId != null ||
        (item.explanation?.isNotEmpty() == true) ||
        !item.summary.isNullOrBlank()
    val context = if (hasContext) ChainStepState.DONE else ChainStepState.PENDING
    val recommendation = ChainStepState.DONE

    val approval = when (item.status) {
        DecisionStatus.PENDING -> ChainStepState.NOW
        DecisionStatus.APPROVED, DecisionStatus.EXECUTED -> ChainStepState.DONE
        DecisionStatus.REJECTED -> ChainStepState.BLOCKED
        DecisionStatus.UNKNOWN -> ChainStepState.PENDING
    }

    val op = linkedOperation(item, operations)
    val approvedLike = item.status == DecisionStatus.APPROVED || item.status == DecisionStatus.EXECUTED

    var order = ChainStepState.PENDING
    var execution = ChainStepState.PENDING
    when {
        item.status == DecisionStatus.REJECTED -> {
            order = ChainStepState.PENDING
            execution = ChainStepState.PENDING
        }
        op != null -> {
            order = ChainStepState.DONE
            execution = when (op.status.uppercase()) {
                "COMPLETED" -> ChainStepState.DONE
                "IN_PROGRESS", "PAUSED" -> ChainStepState.NOW
                else -> ChainStepState.PENDING
            }
        }
        approvedLike -> {
            order = if (item.status == DecisionStatus.EXECUTED) ChainStepState.DONE else ChainStepState.NOW
            execution = if (item.status == DecisionStatus.EXECUTED) ChainStepState.DONE else ChainStepState.PENDING
        }
    }

    val inferred = op != null
    return listOf(
        DerivedChainStep(ChainStepId.SIGNAL, signal),
        DerivedChainStep(ChainStepId.CONTEXT, context),
        DerivedChainStep(ChainStepId.RECOMMENDATION, recommendation),
        DerivedChainStep(ChainStepId.APPROVAL, approval),
        DerivedChainStep(ChainStepId.ORDER, order, inferred = inferred),
        DerivedChainStep(ChainStepId.EXECUTION, execution, inferred = inferred),
    )
}

