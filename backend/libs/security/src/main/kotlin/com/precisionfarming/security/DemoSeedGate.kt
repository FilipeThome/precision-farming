package com.precisionfarming.security

import com.precisionfarming.common.ForbiddenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.HandlerInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Fail-closed HTTP seed reset: ADMIN JWT is not enough.
 * Requires `app.seed=true` and (profile `local` or `allow-demo-secrets`).
 */
@Component
class DemoSeedGate(
    env: Environment,
    props: JwtProperties,
    @Value("\${app.seed:false}") private val seedEnabled: Boolean,
) {
    private val localProfile = env.activeProfiles.contains("local")
    private val allowDemoSecrets = props.allowDemoSecrets

    /** Boot seed and HTTP reset share one gate: app.seed plus local profile or demo secrets. */
    fun permits(): Boolean = seedEnabled && (localProfile || allowDemoSecrets)

    fun requireEnabled() {
        if (!permits()) {
            throw ForbiddenException("Demo seed reset is disabled", "SEED_DISABLED")
        }
    }
}

@Component
class DemoSeedInterceptor(private val gate: DemoSeedGate) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method.equals("POST", ignoreCase = true) &&
            request.requestURI.endsWith("/api/v1/dev/seed/reset")
        ) {
            gate.requireEnabled()
        }
        return true
    }
}

@Component
class DemoSeedWebConfig(private val interceptor: DemoSeedInterceptor) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor)
    }
}
