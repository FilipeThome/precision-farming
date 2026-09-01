package com.precisionfarming.security

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.mock.env.MockEnvironment

class DemoSecretsGuardTest {
    @Test
    fun allowsDemoWhenFlagTrue() {
        val env = MockEnvironment()
        val props = JwtProperties(allowDemoSecrets = true)
        assertDoesNotThrow { DemoSecretsGuard(env, props).run(DefaultApplicationArguments()) }
    }

    @Test
    fun allowsDemoOnLocalProfile() {
        val env = MockEnvironment().withProperty("spring.profiles.active", "local")
        env.setActiveProfiles("local")
        val props = JwtProperties(allowDemoSecrets = false)
        assertDoesNotThrow { DemoSecretsGuard(env, props).run(DefaultApplicationArguments()) }
    }

    @Test
    fun refusesDemoSecretWhenNotAllowed() {
        val env = MockEnvironment()
        env.setProperty("spring.datasource.password", "precision")
        val props = JwtProperties(allowDemoSecrets = false)
        assertThrows(IllegalStateException::class.java) {
            DemoSecretsGuard(env, props).run(DefaultApplicationArguments())
        }
    }
}
