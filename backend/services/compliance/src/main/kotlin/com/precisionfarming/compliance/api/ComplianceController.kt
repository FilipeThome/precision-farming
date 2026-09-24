package com.precisionfarming.compliance.api

import com.precisionfarming.compliance.application.ComplianceService
import com.precisionfarming.compliance.domain.nfeHomologationXml
import com.precisionfarming.security.FarmAccess
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ComplianceController(
    private val svc: ComplianceService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/traceability")
    fun list(@RequestParam(required = false) farmId: UUID?) = svc.listTraceability(farmAccess.current(), farmId)

    @GetMapping("/traceability/{id}")
    fun get(@PathVariable id: UUID) = svc.getTraceability(farmAccess.current(), id)

    @GetMapping("/esg")
    fun esg(@RequestParam(required = false) farmId: UUID?) = svc.listEsg(farmAccess.current(), farmId)

    @GetMapping("/compliance/lots/{lotCode}")
    fun evidencePack(@PathVariable lotCode: String) =
        svc.getEvidencePack(farmAccess.current(), lotCode)

    @GetMapping("/compliance/lots/{lotCode}/nfe", produces = [MediaType.APPLICATION_XML_VALUE])
    fun nfe(@PathVariable lotCode: String): String {
        val pack = svc.getEvidencePack(farmAccess.current(), lotCode)
        return nfeHomologationXml(pack.receituarioNumber, pack.responsibleTechCpf)
    }

    @GetMapping("/compliance/farms/{farmId}/credit-dossier")
    fun creditDossier(@PathVariable farmId: UUID) =
        svc.getCreditDossier(farmAccess.current(), farmId)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class ComplianceSeedController(private val svc: ComplianceService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "compliance").also { svc.seed() }
}
