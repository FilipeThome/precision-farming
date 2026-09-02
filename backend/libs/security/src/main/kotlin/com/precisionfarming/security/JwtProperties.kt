package com.precisionfarming.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
data class JwtProperties(
    val jwtSecret: String = "precision-farming-demo-jwt-secret-key-32",
    val issuer: String = "precision-farming",
    val accessMinutes: Long = 30,
    val refreshDays: Long = 7,
    /** Short-lived inter-service JWTs (saga callers). */
    val serviceAccessMinutes: Long = 2,
    /**
     * When false, DemoSecretsGuard refuses demo JWT/DB defaults unless profile `local`.
     * Default false so non-local deploys fail closed without explicit ALLOW_DEMO_SECRETS=true.
     */
    val allowDemoSecrets: Boolean = false,
)
