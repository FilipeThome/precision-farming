package com.precisionfarming.operation.infrastructure

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

@Component
class InventorySagaClient(
    private val jwtService: JwtService,
    @Value("\${app.clients.inventory}") private val inventoryUrl: String,
) {
    private val http = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(10))
            },
        )
        .build()

    private fun serviceToken(farmId: UUID) =
        jwtService.cachedServiceToken(
            subject = DemoIds.uuid("svc-operation"),
            farmIds = listOf(farmId),
            email = "operation@internal",
        )

    fun move(itemId: UUID, type: String, quantity: BigDecimal, reference: String, farmId: UUID) {
        http.post().uri("$inventoryUrl/api/v1/inventory/movements")
            .header("Authorization", "Bearer ${serviceToken(farmId)}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("itemId" to itemId, "type" to type, "quantity" to quantity, "reference" to reference))
            .retrieve()
            .toBodilessEntity()
    }
}
