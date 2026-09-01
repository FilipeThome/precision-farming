package com.precisionfarming.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
data class JwtProperties(
    val jwtSecret: String = "precision-farming-demo-jwt-secret-key-32",
    val issuer: String = "precision-farming",
    val accessMinutes: Long = 30,
    val refreshDays: Long = 7,
    /** When false, DemoSecretsGuard refuses demo JWT/DB defaults unless profile `local`. */
    val allowDemoSecrets: Boolean = true,
)
