package com.precisionfarming.asset.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.ServiceUnavailableException
import com.precisionfarming.security.CallerBearer
import com.precisionfarming.security.TimedRestClient
import com.precisionfarming.security.UpstreamErrorMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

interface MachinePhotoGuard {
    fun requireAssignable(fileId: UUID, farmId: UUID, machineId: UUID?)
    fun bind(fileId: UUID, machineId: UUID)
    fun unbind(fileId: UUID)
}

@Component
class RemoteMachinePhotoGuard(
    private val fileUrl: String,
    private val http: RestClient,
) : MachinePhotoGuard {
    @Autowired
    constructor(
        @Value("\${app.clients.file:http://localhost:8091}") fileUrl: String,
    ) : this(fileUrl, TimedRestClient.create())

    override fun requireAssignable(fileId: UUID, farmId: UUID, machineId: UUID?) {
        val meta = fetch(fileId)
        if (meta.kind != "MACHINE_PHOTO") {
            throw ConflictException("FILE_KIND_UNSUPPORTED", "File is not a machine photo")
        }
        val fileFarm = meta.farmId ?: throw ServiceUnavailableException()
        UpstreamErrorMapper.requireSameFarm(fileFarm, farmId, "File does not belong to farm")
        when {
            machineId == null && meta.entityId != null ->
                throw ConflictException("FILE_ALREADY_BOUND", "File is already bound")
            machineId != null && meta.entityId != null && meta.entityId != machineId ->
                throw ConflictException("FILE_ALREADY_BOUND", "File is already bound")
        }
    }

    override fun bind(fileId: UUID, machineId: UUID) {
        postBinding(fileId, machineId)
    }

    override fun unbind(fileId: UUID) {
        postBinding(fileId, null)
    }

    private fun fetch(fileId: UUID): FileRef {
        return try {
            http.get().uri("$fileUrl/api/v1/files/$fileId")
                .header("Authorization", CallerBearer.header())
                .retrieve()
                .body(FileRef::class.java)
                ?: UpstreamErrorMapper.missingBody()
        } catch (ex: Exception) {
            UpstreamErrorMapper.map(ex, "FILE_NOT_FOUND", "File not found")
        }
    }

    private fun postBinding(fileId: UUID, entityId: UUID?) {
        try {
            http.post().uri("$fileUrl/api/v1/files/$fileId/binding")
                .header("Authorization", CallerBearer.header())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BindingBody(entityId))
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() == 409) {
                throw ConflictException("FILE_ALREADY_BOUND", "File is already bound")
            }
            UpstreamErrorMapper.map(ex, "FILE_NOT_FOUND", "File not found")
        } catch (ex: Exception) {
            UpstreamErrorMapper.map(ex, "FILE_NOT_FOUND", "File not found")
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class FileRef(val farmId: UUID?, val kind: String?, val entityId: UUID?)

    private data class BindingBody(val entityId: UUID?)
}
