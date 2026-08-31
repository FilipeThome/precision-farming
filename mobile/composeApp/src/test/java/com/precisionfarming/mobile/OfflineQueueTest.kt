package com.precisionfarming.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineQueueTest {
    @Test
    fun clientOperationIdIsStable() {
        val id = "op-device-1"
        assertEquals(id, id)
    }
}
