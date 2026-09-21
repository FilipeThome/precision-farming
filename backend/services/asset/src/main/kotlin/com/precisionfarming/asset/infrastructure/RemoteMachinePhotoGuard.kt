package com.precisionfarming.asset.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.UnauthorizedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Duration
import java.util.UUID

fun interface MachinePhotoGuard {
    fun requireMachinePhoto(fileId: UUID, farmId: UUID)
}

@Component
class RemoteMachinePhotoGuard(
    @Value("\${app.clients.file:http://localhost:8091}") private val fileUrl: String,
) : MachinePhotoGuard {
    private val http = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(10))
            },
        )
        .build()

    override fun requireMachinePhoto(fileId: UUID, farmId: UUID) {
        val meta = fetch(fileId)
        if (meta.kind != "MACHINE_PHOTO") {
            throw ConflictException("FILE_KIND_UNSUPPORTED", "File is not a machine photo")
        }
        if (meta.farmId != farmId) {
            throw ForbiddenException("File does not belong to farm", "FARM_SCOPE_DENIED")
        }
    }

    private fun fetch(fileId: UUID): FileRef {
        return try {
            http.get().uri("$fileUrl/api/v1/files/$fileId")
                .header("Authorization", bearer())
                .retrieve()
                .body(FileRef::class.java)
                ?: throw NotFoundException("FILE_NOT_FOUND", "File not found")
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() == 404) {
                throw NotFoundException("FILE_NOT_FOUND", "File not found")
            }
            throw ForbiddenException("File does not belong to farm", "FARM_SCOPE_DENIED")
        }
    }

    private fun bearer(): String {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: throw UnauthorizedException("Missing bearer token")
        return request.getHeader("Authorization") ?: throw UnauthorizedException("Missing bearer token")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class FileRef(val farmId: UUID?, val kind: String?)
}
