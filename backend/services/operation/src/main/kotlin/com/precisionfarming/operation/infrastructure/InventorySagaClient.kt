package com.precisionfarming.operation.infrastructure

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.util.UUID

@Component
class InventorySagaClient(
    private val jwtService: JwtService,
    @Value("\${app.clients.inventory}") private val inventoryUrl: String,
) {
    private val http = RestClient.create()

    private fun serviceToken() =
        jwtService.cachedAccessToken(DemoIds.uuid("svc-operation"), "operation@internal", "ADMIN")

    fun move(itemId: UUID, type: String, quantity: BigDecimal, reference: String) {
        http.post().uri("$inventoryUrl/api/v1/inventory/movements")
            .header("Authorization", "Bearer ${serviceToken()}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("itemId" to itemId, "type" to type, "quantity" to quantity, "reference" to reference))
            .retrieve()
            .toBodilessEntity()
    }
}
