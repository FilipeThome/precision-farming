package com.precisionfarming.gateway

import com.precisionfarming.common.PemKeys
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class GatewayDemoSecretsGuard(
    private val env: Environment,
    @Value("\${app.security.jwt-public-key}") private val jwtPublicKey: String,
    @Value("\${app.security.allow-demo-secrets:false}") private val allowDemoSecrets: Boolean,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val local = env.activeProfiles.contains("local")
        if (local || allowDemoSecrets) return
        val missing = jwtPublicKey.isBlank()
        val demo = runCatching { PemKeys.isDemoPublicKey(jwtPublicKey) }.getOrDefault(false)
        if (missing || demo) {
            error(
                "Refusing to start gateway with demo/missing JWT public key outside profile 'local'. " +
                    "Set JWT_PUBLIC_KEY or ALLOW_DEMO_SECRETS=true for local-like demos.",
            )
        }
    }
}
