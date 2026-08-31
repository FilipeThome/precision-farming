package com.precisionfarming.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

@Component
class JwtService(private val props: JwtProperties) {
    private val key: SecretKey = Keys.hmacShaKeyFor(props.jwtSecret.toByteArray())
    private val serviceTokens = ConcurrentHashMap<String, CachedToken>(4)

    fun createAccessToken(userId: UUID, email: String, role: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .issuer(props.issuer)
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(props.accessMinutes * 60)))
            .signWith(key)
            .compact()
    }

    fun cachedAccessToken(userId: UUID, email: String, role: String): String {
        val cacheKey = "$userId:$role"
        val now = Instant.now()
        serviceTokens[cacheKey]?.takeIf { now.isBefore(it.validUntil) }?.let { return it.token }
        val token = createAccessToken(userId, email, role)
        val ttl = (props.accessMinutes * 60 - 60).coerceAtLeast(30)
        serviceTokens[cacheKey] = CachedToken(token, now.plusSeconds(ttl))
        return token
    }

    fun createRefreshToken(userId: UUID): String {
        val now = Instant.now()
        return Jwts.builder()
            .issuer(props.issuer)
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(props.refreshDays * 86400)))
            .signWith(key)
            .compact()
    }

    fun parse(token: String) = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    private data class CachedToken(val token: String, val validUntil: Instant)
}
