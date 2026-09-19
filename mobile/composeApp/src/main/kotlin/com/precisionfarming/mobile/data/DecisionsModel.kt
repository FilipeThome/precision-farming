package com.precisionfarming.mobile.data

enum class DecisionSource { PRESCRIPTION, IRRIGATION, AI_INSIGHT, AGRONOMY }

enum class DecisionStatus { PENDING, APPROVED, REJECTED, EXECUTED, UNKNOWN }

data class DecisionQuantity(val value: Double, val unit: String)

data class DecisionCapabilities(val approve: Boolean, val simulate: Boolean)

/** Pure, serializable projection of any decision-like record returned by the gateway. */
data class DecisionItem(
    /** Stable composite id: `$source:$rawId` (safe as a route param). */
    val id: String,
    val rawId: String,
    val source: DecisionSource,
    /** Raw title code/text; UI localizes through DomainLabels. */
    val title: String,
    val quantity: DecisionQuantity? = null,
    val fieldId: String? = null,
    val farmId: String? = null,
    /** Raw entity id for AI insights that could not be resolved to a field. */
    val entityId: String? = null,
    val status: DecisionStatus,
    val rawStatus: String? = null,
    val createdAt: String? = null,
    val approvedAt: String? = null,
    val confidence: Double? = null,
    val score: Double? = null,
    val explanation: List<String>? = null,
    val summary: String? = null,
    val priority: String? = null,
    val model: String? = null,
    val modelVersion: String? = null,
    val capabilities: DecisionCapabilities,
)

data class DecisionSources(
    val prescriptions: List<PrescriptionDto> = emptyList(),
    val irrigation: List<IrrigationRecommendationDto> = emptyList(),
    val agronomy: List<RecommendationDto> = emptyList(),
    val insights: List<InsightDto> = emptyList(),
    val fields: List<FieldDto> = emptyList(),
)

private val PENDING = setOf("DRAFT", "PENDING", "RECOMMENDED", "OPEN", "PROPOSED", "NEW")
private val APPROVED = setOf("APPROVED", "ACCEPTED")
private val REJECTED = setOf("REJECTED", "DISMISSED", "CANCELLED", "CANCELED")
private val EXECUTED = setOf("EXECUTED", "APPLIED", "DONE", "COMPLETED")

fun normalizeStatus(raw: String?): DecisionStatus {
    if (raw.isNullOrBlank()) return DecisionStatus.UNKNOWN
    val value = raw.trim().uppercase()
    return when {
        value in PENDING -> DecisionStatus.PENDING
        value in APPROVED -> DecisionStatus.APPROVED
        value in REJECTED -> DecisionStatus.REJECTED
        value in EXECUTED -> DecisionStatus.EXECUTED
        else -> DecisionStatus.UNKNOWN
    }
}

fun decisionId(source: DecisionSource, rawId: String): String = "${source.name}:$rawId"

fun parseDecisionId(id: String): Pair<DecisionSource, String>? {
    val idx = id.indexOf(':')
    if (idx <= 0) return null
    val source = runCatching { DecisionSource.valueOf(id.substring(0, idx)) }.getOrNull() ?: return null
    return source to id.substring(idx + 1)
}

private fun fieldIndex(fields: List<FieldDto>): Map<String, FieldDto> =
    fields.associateBy { it.id }

private fun optional(value: String?): String? =
    if (value.isNullOrBlank()) null else value

private fun fromPrescription(p: PrescriptionDto, fields: Map<String, FieldDto>): DecisionItem {
    val field = fields[p.fieldId]
    val status = normalizeStatus(p.status)
    return DecisionItem(
        id = decisionId(DecisionSource.PRESCRIPTION, p.id),
        rawId = p.id,
        source = DecisionSource.PRESCRIPTION,
        title = p.product,
        quantity = if (p.rate.isFinite()) DecisionQuantity(p.rate, p.unit) else null,
        fieldId = optional(p.fieldId),
        farmId = optional(p.farmId) ?: field?.farmId,
        status = status,
        rawStatus = optional(p.status),
        createdAt = optional(p.createdAt),
        approvedAt = optional(p.approvedAt),
        capabilities = DecisionCapabilities(approve = status == DecisionStatus.PENDING, simulate = false),
    )
}

private fun fromIrrigation(r: IrrigationRecommendationDto, fields: Map<String, FieldDto>): DecisionItem {
    val field = r.fieldId?.let { fields[it] }
    val status = normalizeStatus(r.status)
    val mm = r.recommendedMm
    return DecisionItem(
        id = decisionId(DecisionSource.IRRIGATION, r.id),
        rawId = r.id,
        source = DecisionSource.IRRIGATION,
        title = r.reason ?: "IRRIGATION",
        quantity = if (mm != null && mm.isFinite()) DecisionQuantity(mm, "mm") else null,
        fieldId = optional(r.fieldId),
        farmId = optional(r.farmId) ?: field?.farmId,
        status = status,
        rawStatus = optional(r.status),
        createdAt = optional(r.windowStart),
        summary = optional(r.reason),
        capabilities = DecisionCapabilities(approve = false, simulate = status == DecisionStatus.PENDING),
    )
}

private fun fromAgronomy(r: RecommendationDto, fields: Map<String, FieldDto>): DecisionItem {
    val field = r.fieldId?.let { fields[it] }
    return DecisionItem(
        id = decisionId(DecisionSource.AGRONOMY, r.id),
        rawId = r.id,
        source = DecisionSource.AGRONOMY,
        title = r.title ?: r.kind ?: "RECOMMENDATION",
        fieldId = optional(r.fieldId),
        farmId = optional(r.farmId) ?: field?.farmId,
        status = normalizeStatus(r.status),
        rawStatus = optional(r.status),
        createdAt = optional(r.createdAt),
        priority = optional(r.priority),
        summary = optional(r.summary),
        capabilities = DecisionCapabilities(approve = false, simulate = false),
    )
}

private fun fromInsight(i: InsightDto, fields: Map<String, FieldDto>): DecisionItem {
    val field = i.entityId?.let { fields[it] }
    return DecisionItem(
        id = decisionId(DecisionSource.AI_INSIGHT, i.id),
        rawId = i.id,
        source = DecisionSource.AI_INSIGHT,
        title = i.type,
        fieldId = field?.id,
        farmId = field?.farmId,
        entityId = optional(i.entityId),
        status = DecisionStatus.UNKNOWN,
        createdAt = optional(i.generatedAt),
        confidence = i.confidence?.takeIf { it.isFinite() },
        score = i.score.takeIf { it.isFinite() },
        explanation = i.explanation.filter { it.isNotBlank() }.ifEmpty { null },
        model = optional(i.model),
        modelVersion = optional(i.modelVersion),
        capabilities = DecisionCapabilities(approve = false, simulate = false),
    )
}

/** Adapts every decision-like record into a unified list. Pure: no I/O, no mutations. */
fun toDecisionItems(sources: DecisionSources): List<DecisionItem> {
    val fields = fieldIndex(sources.fields)
    return sources.prescriptions.map { fromPrescription(it, fields) } +
        sources.irrigation.map { fromIrrigation(it, fields) } +
        sources.agronomy.map { fromAgronomy(it, fields) } +
        sources.insights.map { fromInsight(it, fields) }
}

const val LOW_CONFIDENCE = 0.6

fun needsHumanReview(item: DecisionItem): Boolean =
    item.confidence != null && item.confidence < LOW_CONFIDENCE
