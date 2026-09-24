package com.precisionfarming.operation.application

import java.util.UUID

fun interface OperationEvents {
    fun operationStarted(operationId: UUID, farmId: UUID, fieldId: UUID, prescriptionId: UUID?)
}

object NoOpOperationEvents : OperationEvents {
    override fun operationStarted(operationId: UUID, farmId: UUID, fieldId: UUID, prescriptionId: UUID?) = Unit
}

/** Pure JSON payload for `precision.operation.started` — no Kafka types. */
fun operationStartedJson(
    operationId: UUID,
    farmId: UUID,
    fieldId: UUID,
    prescriptionId: UUID?,
): String {
    val rx = prescriptionId?.let { "\"$it\"" } ?: "null"
    return """{"event":"operation.started","operationId":"$operationId","farmId":"$farmId","fieldId":"$fieldId","prescriptionId":$rx}"""
}
