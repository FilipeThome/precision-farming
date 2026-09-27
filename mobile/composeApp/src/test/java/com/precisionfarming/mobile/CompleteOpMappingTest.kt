package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.completeOpBody
import com.precisionfarming.mobile.data.offline.OpCommandType
import com.precisionfarming.mobile.data.offline.QueuedCommand
import com.precisionfarming.mobile.data.offline.SyncState
import com.precisionfarming.mobile.data.offline.auditPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompleteOpMappingTest {
    @Test
    fun completeOpBodyIncludesActualLiters() {
        val body = completeOpBody(12.5)
        assertEquals(12.5, body!!.actualLiters, 0.0)
        assertNull(completeOpBody(null))
    }

    @Test
    fun auditPayloadIncludesActualLitersAsString() {
        val cmd = QueuedCommand(
            clientOperationId = "cid-1",
            operationId = "op-1",
            type = OpCommandType.COMPLETE,
            createdAt = "2026-01-01T00:00:00Z",
            state = SyncState.PENDING,
            actualLiters = 7.25,
        )
        assertEquals(
            mapOf("operationId" to "op-1", "actualLiters" to "7.25"),
            auditPayload(cmd),
        )
    }

    @Test
    fun auditPayloadKeepsReasonAndOmitsNullLiters() {
        val pause = QueuedCommand(
            clientOperationId = "cid-2",
            operationId = "op-2",
            type = OpCommandType.PAUSE,
            reason = "chuva",
            createdAt = "2026-01-01T00:00:00Z",
        )
        assertEquals(
            mapOf("operationId" to "op-2", "reason" to "chuva"),
            auditPayload(pause),
        )
    }
}
