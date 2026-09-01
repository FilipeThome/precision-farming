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
    private val serviceTokens = ConcurrentHashMap<String, CachedToken>(8)

    fun createAccessToken(
        userId: UUID,
        email: String,
        role: String,
        tenantId: UUID = DemoTenant.ID,
        farmIds: Collection<UUID> = DemoFarmDirectory.forRole(role),
    ): String {
        val now = Instant.now()
        return Jwts.builder()
            .issuer(props.issuer)
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("tenantId", tenantId.toString())
            .claim("farmIds", farmIds.map { it.toString() })
            .claim("type", JwtAccessType.ACCESS)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(props.accessMinutes * 60)))
            .signWith(key)
            .compact()
    }

    fun createServiceToken(
        subject: UUID,
        farmIds: Collection<UUID>,
        tenantId: UUID = DemoTenant.ID,
        email: String = "service@internal",
    ): String {
        require(farmIds.isNotEmpty()) { "service token requires farmIds" }
        val now = Instant.now()
        return Jwts.builder()
            .issuer(props.issuer)
            .subject(subject.toString())
            .claim("email", email)
            .claim("role", "SERVICE")
            .claim("tenantId", tenantId.toString())
            .claim("farmIds", farmIds.map { it.toString() })
            .claim("type", JwtAccessType.SERVICE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(props.serviceAccessMinutes * 60)))
            .signWith(key)
            .compact()
    }

    fun cachedServiceToken(
        subject: UUID,
        farmIds: Collection<UUID>,
        tenantId: UUID = DemoTenant.ID,
        email: String = "service@internal",
    ): String {
        val cacheKey = "svc:$subject:${farmIds.sorted().joinToString()}"
        val now = Instant.now()
        serviceTokens[cacheKey]?.takeIf { now.isBefore(it.validUntil) }?.let { return it.token }
        val token = createServiceToken(subject, farmIds, tenantId, email)
        val ttl = (props.serviceAccessMinutes * 60 - 15).coerceAtLeast(15)
        serviceTokens[cacheKey] = CachedToken(token, now.plusSeconds(ttl))
        return token
    }

    fun createRefreshToken(userId: UUID): String {
        val now = Instant.now()
        return Jwts.builder()
            .issuer(props.issuer)
            .subject(userId.toString())
            .claim("type", JwtAccessType.REFRESH)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(props.refreshDays * 86400)))
            .signWith(key)
            .compact()
    }

    fun parse(token: String) = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    private data class CachedToken(val token: String, val validUntil: Instant)
}
