package com.precisionfarming.file

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FileDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("file-001"), DemoIds.uuid("file-001"))
    }
}
