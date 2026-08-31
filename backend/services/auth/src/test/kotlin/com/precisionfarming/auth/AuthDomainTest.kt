package com.precisionfarming.auth

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("auth-001"), DemoIds.uuid("auth-001"))
    }
}
