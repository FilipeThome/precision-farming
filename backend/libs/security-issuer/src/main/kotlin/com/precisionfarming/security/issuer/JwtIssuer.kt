package com.precisionfarming.security.issuer

import com.precisionfarming.common.PemKeys
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.security.JwtAccessType
import com.precisionfarming.security.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Signs JWTs. Must only be constructed by auth-service (no Spring @Component).
 */
class JwtIssuer(props: JwtProperties) {
    private val privateKey = PemKeys.parsePrivate(props.jwtPrivateKey)
    private val publicKey = PemKeys.parsePublic(props.jwtPublicKey)
    private val issuer = props.issuer
    private val accessMinutes = props.accessMinutes
    private val refreshDays = props.refreshDays
    private val serviceAccessMinutes = props.serviceAccessMinutes

    fun createAccessToken(
        userId: UUID,
        email: String,
        role: String,
        farmIds: Collection<UUID>,
        tenantId: UUID = DemoTenant.ID,
    ): String {
        require(farmIds.isNotEmpty()) { "access token requires farmIds" }
        val now = Instant.now()
        return Jwts.builder()
            .issuer(issuer)
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("tenantId", tenantId.toString())
            .claim("farmIds", farmIds.map { it.toString() })
            .claim("type", JwtAccessType.ACCESS)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessMinutes * 60)))
            .signWith(privateKey)
            .compact()
    }

    fun createRefreshToken(userId: UUID): IssuedRefresh {
        val now = Instant.now()
        val jti = UUID.randomUUID()
        val expires = now.plusSeconds(refreshDays * 86400)
        val token = Jwts.builder()
            .issuer(issuer)
            .subject(userId.toString())
            .id(jti.toString())
            .claim("type", JwtAccessType.REFRESH)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expires))
            .signWith(privateKey)
            .compact()
        return IssuedRefresh(token, jti, expires)
    }

    fun createServiceToken(
        subject: UUID,
        farmIds: Collection<UUID>,
        tenantId: UUID = DemoTenant.ID,
        email: String = "service@internal",
    ): String {
        require(farmIds.isNotEmpty()) { "service token requires farmIds" }
        require(farmIds.size <= MAX_SERVICE_FARMS) { "service token farmIds cap exceeded" }
        val now = Instant.now()
        return Jwts.builder()
            .issuer(issuer)
            .subject(subject.toString())
            .claim("email", email)
            .claim("role", "SERVICE")
            .claim("tenantId", tenantId.toString())
            .claim("farmIds", farmIds.map { it.toString() })
            .claim("type", JwtAccessType.SERVICE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(serviceAccessMinutes * 60)))
            .signWith(privateKey)
            .compact()
    }

    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).payload

    data class IssuedRefresh(val token: String, val jti: UUID, val expiresAt: Instant)

    companion object {
        const val MAX_SERVICE_FARMS = 32
    }
}
