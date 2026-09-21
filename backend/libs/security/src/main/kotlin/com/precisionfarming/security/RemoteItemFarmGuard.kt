package com.precisionfarming.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

interface ItemFarmGuard {
    fun requireBelongsToFarm(itemId: UUID, farmId: UUID)
}

@Component
@ConditionalOnProperty(prefix = "app.clients", name = ["inventory"])
class RemoteItemFarmGuard(
    private val inventoryUrl: String,
    private val http: RestClient,
) : ItemFarmGuard {
    @Autowired
    constructor(@Value("\${app.clients.inventory}") inventoryUrl: String) : this(inventoryUrl, TimedRestClient.create())

    override fun requireBelongsToFarm(itemId: UUID, farmId: UUID) {
        val item = fetchItem(itemId)
        UpstreamErrorMapper.requireSameFarm(item.farmId, farmId, "Item does not belong to farm")
    }

    private fun fetchItem(itemId: UUID): ItemRef {
        return try {
            http.get().uri("$inventoryUrl/api/v1/inventory/$itemId")
                .header("Authorization", CallerBearer.header())
                .retrieve()
                .body(ItemRef::class.java)
                ?: UpstreamErrorMapper.missingBody()
        } catch (ex: Exception) {
            UpstreamErrorMapper.map(ex, "ITEM_NOT_FOUND", "Item not found")
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ItemRef(val farmId: UUID)
}
