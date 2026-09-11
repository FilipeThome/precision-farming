package com.precisionfarming.security

import com.precisionfarming.common.ForbiddenException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class DemoSeedGateTest {
    @Test
    fun rejectsWhenSeedDisabled() {
        val ex = assertThrows(ForbiddenException::class.java) {
            gate(seed = false, local = true, allowDemo = true).requireEnabled()
        }
        assertEquals("SEED_DISABLED", ex.code)
    }

    @Test
    fun rejectsWhenNonLocalAndDemoSecretsDisallowed() {
        val ex = assertThrows(ForbiddenException::class.java) {
            gate(seed = true, local = false, allowDemo = false).requireEnabled()
        }
        assertEquals("SEED_DISABLED", ex.code)
    }

    @Test
    fun allowsLocalWhenSeedEnabled() {
        assertDoesNotThrow { gate(seed = true, local = true, allowDemo = false).requireEnabled() }
    }

    @Test
    fun allowsDemoSecretsWhenSeedEnabled() {
        assertDoesNotThrow { gate(seed = true, local = false, allowDemo = true).requireEnabled() }
    }

    private fun gate(seed: Boolean, local: Boolean, allowDemo: Boolean): DemoSeedGate {
        val env = MockEnvironment()
        if (local) env.setActiveProfiles("local")
        return DemoSeedGate(env, JwtProperties(allowDemoSecrets = allowDemo), seed)
    }
}
