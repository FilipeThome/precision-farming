package com.precisionfarming.security

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DemoSecretsGuard(
    private val env: Environment,
    private val props: JwtProperties,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val local = env.activeProfiles.contains("local")
        if (local || props.allowDemoSecrets) return
        val demoSecret = props.jwtSecret == DEMO_JWT
        val demoDb = env.getProperty("spring.datasource.password") == DEMO_DB_PASSWORD
        if (demoSecret || demoDb) {
            error(
                "Refusing to start with demo JWT/DB secrets outside profile 'local'. " +
                    "Set real secrets or app.security.allow-demo-secrets=true for local-like demos.",
            )
        }
    }

    companion object {
        const val DEMO_JWT = "precision-farming-demo-jwt-secret-key-32"
        const val DEMO_DB_PASSWORD = "precision"
    }
}
