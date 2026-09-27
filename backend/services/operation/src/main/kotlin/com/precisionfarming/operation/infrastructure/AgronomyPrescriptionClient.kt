package com.precisionfarming.operation.infrastructure

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.UpstreamErrorMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class PrescriptionRef(
    val id: UUID,
    val farmId: UUID,
    val fieldId: UUID,
    val status: String,
)

@Component
class AgronomyPrescriptionClient(
    @Value("\${app.clients.agronomy}") private val agronomyUrl: String,
    @Value("\${app.clients.auth}") private val authUrl: String,
    @Value("\${app.security.service-mint-secret}") private val mintSecret: String,
) {
    private val http = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(10))
            },
        )
        .build()
    private val cache = ConcurrentHashMap<String, CachedToken>()

    fun get(prescriptionId: UUID, farmId: UUID): PrescriptionRef {
        return try {
            http.get().uri("$agronomyUrl/api/v1/prescriptions/$prescriptionId")
                .header("Authorization", "Bearer ${serviceToken(farmId)}")
                .retrieve()
                .body(PrescriptionRef::class.java)
                ?: UpstreamErrorMapper.missingBody()
        } catch (ex: Exception) {
            UpstreamErrorMapper.map(ex, "PRESCRIPTION_NOT_FOUND", "Prescription not found")
        }
    }

    private fun serviceToken(farmId: UUID): String {
        val key = farmId.toString()
        val now = Instant.now()
        cache[key]?.takeIf { now.isBefore(it.validUntil) }?.let { return it.token }
        val body = mapOf(
            "subject" to DemoIds.uuid("svc-operation"),
            "farmIds" to listOf(farmId),
            "email" to "operation@internal",
        )
        val minted = http.post().uri("$authUrl/internal/service-tokens")
            .header("X-Service-Mint", mintSecret)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(MintedToken::class.java)
            ?: error("auth mint returned empty body")
        cache[key] = CachedToken(minted.accessToken, now.plusSeconds(105))
        return minted.accessToken
    }

    private data class MintedToken(val accessToken: String, val tokenType: String? = null)
    private data class CachedToken(val token: String, val validUntil: Instant)
}
