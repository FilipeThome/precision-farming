package com.precisionfarming.compliance

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComplianceDomainTest {
    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("compliance-001"), DemoIds.uuid("compliance-001"))
    }
}
