package com.precisionfarming.farm.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.UUID

@Component
class AuthMembershipClient(
    @Value("\${app.clients.auth}") private val authUrl: String,
    @Value("\${app.security.auth-internal-secret}") private val secret: String,
) {
    private val http = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(10))
            },
        )
        .build()

    fun grant(userId: UUID, farmId: UUID) {
        http.post().uri("$authUrl/internal/memberships")
            .header("X-Auth-Internal", secret)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("userId" to userId, "farmId" to farmId))
            .retrieve()
            .toBodilessEntity()
    }

    fun revoke(farmId: UUID, userId: UUID? = null) {
        val uri = if (userId != null) {
            "$authUrl/internal/memberships/revoke?farmId=$farmId&userId=$userId"
        } else {
            "$authUrl/internal/memberships/revoke?farmId=$farmId"
        }
        http.post().uri(uri)
            .header("X-Auth-Internal", secret)
            .retrieve()
            .toBodilessEntity()
    }
}
