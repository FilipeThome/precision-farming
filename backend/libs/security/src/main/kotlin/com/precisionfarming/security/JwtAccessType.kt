package com.precisionfarming.security

import org.springframework.security.oauth2.jwt.JwtException

object JwtAccessType {
    const val CLAIM = "type"
    const val ACCESS = "access"
    const val REFRESH = "refresh"
    const val SERVICE = "service"

    fun requireAccess(type: String?) {
        if (!ACCESS.equals(type, ignoreCase = true)) {
            throw JwtException("Access token required")
        }
    }

    /** Human access or inter-service token (not refresh). */
    fun requireResourceToken(type: String?) {
        val ok = ACCESS.equals(type, ignoreCase = true) || SERVICE.equals(type, ignoreCase = true)
        if (!ok) {
            throw JwtException("Access or service token required")
        }
    }
}
