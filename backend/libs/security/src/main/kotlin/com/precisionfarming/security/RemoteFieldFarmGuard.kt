package com.precisionfarming.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

interface FieldFarmGuard {
    fun requireBelongsToFarm(fieldId: UUID, farmId: UUID)
    fun requireRead(scope: AccessScope, fieldId: UUID)
}

@Component
@ConditionalOnProperty(prefix = "app.clients", name = ["farm"])
class RemoteFieldFarmGuard(
    private val farmUrl: String,
    private val http: RestClient,
) : FieldFarmGuard {
    @Autowired
    constructor(@Value("\${app.clients.farm}") farmUrl: String) : this(farmUrl, TimedRestClient.create())

    override fun requireBelongsToFarm(fieldId: UUID, farmId: UUID) {
        val field = fetchField(fieldId)
        UpstreamErrorMapper.requireSameFarm(field.farmId, farmId, "Field does not belong to farm")
    }

    override fun requireRead(scope: AccessScope, fieldId: UUID) {
        val field = fetchField(fieldId)
        scope.requireFarmRead(field.farmId, "FIELD_NOT_FOUND", "Not found")
    }

    private fun fetchField(fieldId: UUID): FieldRef {
        return try {
            http.get().uri("$farmUrl/api/v1/fields/$fieldId")
                .header("Authorization", CallerBearer.header())
                .retrieve()
                .body(FieldRef::class.java)
                ?: UpstreamErrorMapper.missingBody()
        } catch (ex: Exception) {
            UpstreamErrorMapper.map(ex, "FIELD_NOT_FOUND", "Field not found")
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class FieldRef(val farmId: UUID)
}
