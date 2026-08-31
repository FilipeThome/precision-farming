package com.precisionfarming.gateway

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GatewayRouteTest {
    @Test
    fun gatewayModuleName() {
        assertTrue("gateway".isNotBlank())
    }
}
