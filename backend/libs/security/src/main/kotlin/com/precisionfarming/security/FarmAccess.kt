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
        val tenantId = runCatching { UUID.fromString(tenantRaw) }.getOrDefault(DemoTenant.ID)
        val claimed = jwt.getClaimAsStringList("farmIds")
            ?.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.toSet()
            .orEmpty()
        if (claimed.isEmpty()) {
            throw UnauthorizedException("Missing farmIds claim")
        }
        // JWT claims + runtime-created farms for this role (seed IDs stay in the token).
        val runtimeCreated = DemoFarmDirectory.forRole(role) - DemoFarmDirectory.ALL
        val userId = runCatching { UUID.fromString(jwt.subject) }.getOrNull()
        return AccessScope(
            tenantId = tenantId,
            farmIds = claimed + runtimeCreated,
            role = role,
            userId = userId,
        )
    }
}
