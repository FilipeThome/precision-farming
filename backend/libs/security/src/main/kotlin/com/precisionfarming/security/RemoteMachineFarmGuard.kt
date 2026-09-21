package com.precisionfarming.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

interface MachineFarmGuard {
    fun requireBelongsToFarm(machineId: UUID, farmId: UUID)
    fun requireRead(scope: AccessScope, machineId: UUID)
}

@Component
@ConditionalOnProperty(prefix = "app.clients", name = ["asset"])
class RemoteMachineFarmGuard(
    private val assetUrl: String,
    private val http: RestClient,
) : MachineFarmGuard {
    @Autowired
    constructor(@Value("\${app.clients.asset}") assetUrl: String) : this(assetUrl, TimedRestClient.create())

    override fun requireBelongsToFarm(machineId: UUID, farmId: UUID) {
        val machine = fetchMachine(machineId)
        UpstreamErrorMapper.requireSameFarm(machine.farmId, farmId, "Machine does not belong to farm")
    }

    override fun requireRead(scope: AccessScope, machineId: UUID) {
        val machine = fetchMachine(machineId)
        scope.requireFarmRead(machine.farmId, "MACHINE_NOT_FOUND", "Not found")
    }

    private fun fetchMachine(machineId: UUID): MachineRef {
        return try {
            http.get().uri("$assetUrl/api/v1/machines/$machineId")
                .header("Authorization", CallerBearer.header())
                .retrieve()
                .body(MachineRef::class.java)
                ?: UpstreamErrorMapper.missingBody()
        } catch (ex: Exception) {
            UpstreamErrorMapper.map(ex, "MACHINE_NOT_FOUND", "Machine not found")
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class MachineRef(val farmId: UUID)
}
