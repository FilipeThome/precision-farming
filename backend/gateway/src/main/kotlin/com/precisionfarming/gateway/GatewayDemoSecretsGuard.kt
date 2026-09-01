package com.precisionfarming.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/** Mirrors DemoSecretsGuard without depending on servlet security lib. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class GatewayDemoSecretsGuard(
    private val env: Environment,
    @Value("\${app.security.jwt-secret}") private val jwtSecret: String,
    @Value("\${app.security.allow-demo-secrets:false}") private val allowDemoSecrets: Boolean,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val local = env.activeProfiles.contains("local")
        if (local || allowDemoSecrets) return
        val demoSecret = jwtSecret == DEMO_JWT
        val weakSecret = jwtSecret.toByteArray(Charsets.UTF_8).size < 32
        if (demoSecret || weakSecret) {
            error(
                "Refusing to start gateway with demo/weak JWT outside profile 'local'. " +
                    "Set JWT_SECRET or ALLOW_DEMO_SECRETS=true for local-like demos.",
            )
        }
    }

    companion object {
        const val DEMO_JWT = "precision-farming-demo-jwt-secret-key-32"
    }
}
