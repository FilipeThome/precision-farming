package com.precisionfarming.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.UnauthorizedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Duration
import java.util.UUID

fun interface FieldFarmGuard {
    fun requireBelongsToFarm(fieldId: UUID, farmId: UUID)
}

@Component
@ConditionalOnProperty(prefix = "app.clients", name = ["farm"])
class RemoteFieldFarmGuard(
    @Value("\${app.clients.farm}") private val farmUrl: String,
) : FieldFarmGuard {
    private val http = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(10))
            },
        )
        .build()

    override fun requireBelongsToFarm(fieldId: UUID, farmId: UUID) {
        val mapped = DemoFieldFarms.farmId(fieldId)
        if (mapped != null) {
            if (mapped != farmId) {
                throw ForbiddenException("Field does not belong to farm", "FARM_SCOPE_DENIED")
            }
            return
        }
        val field = fetchField(fieldId)
        if (field.farmId != farmId) {
            throw ForbiddenException("Field does not belong to farm", "FARM_SCOPE_DENIED")
        }
        DemoFieldFarms.register(fieldId, field.farmId)
    }

    private fun fetchField(fieldId: UUID): FieldRef {
        return try {
            http.get().uri("$farmUrl/api/v1/fields/$fieldId")
                .header("Authorization", bearer())
                .retrieve()
                .body(FieldRef::class.java)
                ?: throw NotFoundException("FIELD_NOT_FOUND", "Field not found")
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() == 404) {
                throw NotFoundException("FIELD_NOT_FOUND", "Field not found")
            }
            throw ForbiddenException("Field does not belong to farm", "FARM_SCOPE_DENIED")
        }
    }

    private fun bearer(): String {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: throw UnauthorizedException("Missing bearer token")
        return request.getHeader("Authorization") ?: throw UnauthorizedException("Missing bearer token")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class FieldRef(val farmId: UUID)
}
