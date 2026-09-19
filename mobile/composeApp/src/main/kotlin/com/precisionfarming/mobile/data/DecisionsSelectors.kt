package com.precisionfarming.mobile.data

private val PRIORITY_RANK = mapOf(
    "CRITICAL" to 0,
    "HIGH" to 1,
    "MEDIUM" to 2,
    "LOW" to 3,
)

private fun priorityRank(item: DecisionItem): Int {
    val key = item.priority?.uppercase()
    return if (key != null && key in PRIORITY_RANK) PRIORITY_RANK.getValue(key) else 4
}

private fun statusRank(status: DecisionStatus): Int = when (status) {
    DecisionStatus.PENDING -> 0
    DecisionStatus.UNKNOWN -> 1
    DecisionStatus.APPROVED -> 2
    DecisionStatus.EXECUTED -> 3
    DecisionStatus.REJECTED -> 4
}

/** Decisions that still need a human: pending approvals. */
fun pendingDecisions(items: List<DecisionItem>): List<DecisionItem> =
    items.filter { it.status == DecisionStatus.PENDING }

/** Items that require attention: pending approvals + low-confidence insights. */
fun actionableDecisions(items: List<DecisionItem>): List<DecisionItem> =
    items.filter { it.status == DecisionStatus.PENDING || needsHumanReview(it) }

/** Severity/priority first, then pending status, then low confidence, then newest first. */
fun sortByPriority(items: List<DecisionItem>): List<DecisionItem> =
    items.sortedWith { a, b ->
        val p = priorityRank(a) - priorityRank(b)
        if (p != 0) return@sortedWith p
        val s = statusRank(a.status) - statusRank(b.status)
        if (s != 0) return@sortedWith s
        val review = (if (needsHumanReview(b)) 1 else 0) - (if (needsHumanReview(a)) 1 else 0)
        if (review != 0) return@sortedWith review
        (b.createdAt ?: "").compareTo(a.createdAt ?: "")
    }

enum class DecisionFilter { PENDING, APPROVED, ALL }

fun filterDecisions(items: List<DecisionItem>, filter: DecisionFilter): List<DecisionItem> = when (filter) {
    DecisionFilter.PENDING -> pendingDecisions(items)
    DecisionFilter.APPROVED -> items.filter {
        it.status == DecisionStatus.APPROVED || it.status == DecisionStatus.EXECUTED
    }
    DecisionFilter.ALL -> items
}
