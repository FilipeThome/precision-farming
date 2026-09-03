package com.precisionfarming.security

import com.precisionfarming.common.PemKeys
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
        val app = env.getProperty("spring.application.name").orEmpty()
        val isAuth = app == "auth-service"
        if (props.jwtPrivateKey.isNotBlank() && !isAuth) {
            error("JWT private key is only allowed on auth-service")
        }
        val local = env.activeProfiles.contains("local")
        if (local || props.allowDemoSecrets) return
        val missingPublic = props.jwtPublicKey.isBlank()
        val demoPublic = runCatching { PemKeys.isDemoPublicKey(props.jwtPublicKey) }.getOrDefault(false)
        val demoDb = env.getProperty("spring.datasource.password") == DEMO_DB_PASSWORD
        if (missingPublic || demoPublic || demoDb) {
            error(
                "Refusing to start with demo/weak JWT or demo DB secrets outside profile 'local'. " +
                    "Set JWT_PUBLIC_KEY (and auth JWT_PRIVATE_KEY) or ALLOW_DEMO_SECRETS=true.",
            )
        }
        if (isAuth && (props.serviceMintSecret.length < 32 || props.authInternalSecret.length < 32)) {
            error("Auth mint/internal secrets must be at least 32 characters outside local/demo.")
        }
    }

    companion object {
        const val DEMO_DB_PASSWORD = "precision"
    }
}
