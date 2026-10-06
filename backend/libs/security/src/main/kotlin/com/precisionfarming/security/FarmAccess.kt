package com.precisionfarming.security

import com.precisionfarming.common.UnauthorizedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FarmAccess {
    fun current(): AccessScope {
        val auth = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: throw UnauthorizedException("Missing bearer token")
        return fromJwt(auth.token)
    }

    fun fromJwt(jwt: Jwt): AccessScope {
        val role = jwt.getClaimAsString("role") ?: "OPERATOR"
        val tenantRaw = jwt.getClaimAsString("tenantId")
        val tenantId = resolveTenant(tenantRaw)
        val claimed = jwt.getClaimAsStringList("farmIds")
            ?.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.toSet()
            .orEmpty()
        val userId = runCatching { UUID.fromString(jwt.subject) }.getOrNull()
        val farmIds = claimed
        if (farmIds.isEmpty()) {
            throw UnauthorizedException("Missing farmIds claim")
        }
        return AccessScope(
            tenantId = tenantId,
            farmIds = farmIds,
            role = role,
            userId = userId,
        )
    }

    private fun resolveTenant(tenantRaw: String?): UUID {
        if (tenantRaw.isNullOrBlank()) return DemoTenant.ID
        val parsed = runCatching { UUID.fromString(tenantRaw) }.getOrNull()
            ?: throw UnauthorizedException("Invalid tenant")
        if (parsed != DemoTenant.ID) throw UnauthorizedException("Invalid tenant")
        return parsed
    }
}
