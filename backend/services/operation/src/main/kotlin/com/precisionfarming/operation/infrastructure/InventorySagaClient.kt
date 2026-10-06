package com.precisionfarming.operation.infrastructure

import com.precisionfarming.common.DemoIds
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class InventoryStepResult { Applied, AlreadyApplied, Skipped }

@Component
class InventorySagaClient(
    @Value("\${app.clients.inventory}") private val inventoryUrl: String,
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

    fun move(
        itemId: UUID,
        type: String,
        quantity: BigDecimal,
        reference: String,
        farmId: UUID,
        stepKey: String,
    ): InventoryStepResult {
        return postMovement(
            farmId,
            mapOf(
                "itemId" to itemId,
                "type" to type,
                "quantity" to quantity,
                "reference" to reference,
                "stepKey" to stepKey,
            ),
        )
    }

    private fun postMovement(farmId: UUID, body: Map<String, Any?>): InventoryStepResult {
        try {
            http.post().uri("$inventoryUrl/api/v1/inventory/movements")
                .header("Authorization", "Bearer ${serviceToken(farmId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
            return InventoryStepResult.Applied
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() == 409 && ex.responseBodyAsString.contains("\"STEP_ALREADY_APPLIED\"")) {
                return InventoryStepResult.AlreadyApplied
            }
            throw ex
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
