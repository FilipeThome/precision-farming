package com.precisionfarming.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
data class JwtProperties(
    /** X.509 PEM (literal newlines or `\n`). Required on every verifier. */
    val jwtPublicKey: String = "",
    /** PKCS#8 PEM. Auth-service only; must be empty elsewhere. */
    val jwtPrivateKey: String = "",
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
    /** Auth+operation mint header. Empty on other services. */
    val serviceMintSecret: String = "",
    /** Auth+farm membership header. Empty on other services. */
    val authInternalSecret: String = "",
    /** Regex of proxy addresses allowed to supply X-Forwarded-For (auth rate limit). */
    val trustedProxies: String = "127\\.0\\.0\\.1|::1|10\\..*|192\\.168\\..*|172\\.(1[6-9]|2[0-9]|3[0-1])\\..*",
)
