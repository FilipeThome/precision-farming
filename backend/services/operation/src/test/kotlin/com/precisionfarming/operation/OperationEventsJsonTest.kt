package com.precisionfarming.operation

import com.precisionfarming.operation.application.operationStartedJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationEventsJsonTest {
    @Test
    fun payloadWithPrescriptionId() {
        val op = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val farm = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val field = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val rx = UUID.fromString("44444444-4444-4444-4444-444444444444")
        assertEquals(
            """{"event":"operation.started","operationId":"$op","farmId":"$farm","fieldId":"$field","prescriptionId":"$rx"}""",
            operationStartedJson(op, farm, field, rx),
        )
    }

    @Test
    fun payloadWithNullPrescriptionId() {
        val op = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val farm = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val field = UUID.fromString("33333333-3333-3333-3333-333333333333")
        assertEquals(
            """{"event":"operation.started","operationId":"$op","farmId":"$farm","fieldId":"$field","prescriptionId":null}""",
            operationStartedJson(op, farm, field, null),
        )
    }
}
