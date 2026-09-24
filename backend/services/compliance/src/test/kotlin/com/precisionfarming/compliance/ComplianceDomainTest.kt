package com.precisionfarming.compliance

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.compliance.application.ComplianceService
import com.precisionfarming.compliance.domain.DEFORESTATION_CUTOFF
import com.precisionfarming.compliance.domain.nfeHomologationXml
import com.precisionfarming.compliance.infrastructure.CreditDossierEntity
import com.precisionfarming.compliance.infrastructure.CreditDossierJpaRepository
import com.precisionfarming.compliance.infrastructure.EsgMetricJpaRepository
import com.precisionfarming.compliance.infrastructure.EvidencePackEntity
import com.precisionfarming.compliance.infrastructure.EvidencePackJpaRepository
import com.precisionfarming.compliance.infrastructure.TraceabilityJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class ComplianceDomainTest {
    private val traces = mockk<TraceabilityJpaRepository>()
    private val esg = mockk<EsgMetricJpaRepository>()
    private val evidence = mockk<EvidencePackJpaRepository>()
    private val dossiers = mockk<CreditDossierJpaRepository>()
    private val svc = ComplianceService(traces, esg, evidence, dossiers)

    @Test
    fun demoIdsAreStable() {
        assertEquals(DemoIds.uuid("compliance-001"), DemoIds.uuid("compliance-001"))
    }

    @Test
    fun evidencePackHappyPath() {
        val farmId = DemoIds.uuid("farm-001")
        val pack = EvidencePackEntity(
            lotCode = "LOT-BV-001",
            farmId = farmId,
            farmName = "Boa Vista",
            fieldId = DemoIds.uuid("field-001"),
            fieldName = "Field 001",
            polygonGeoJson = "{}",
            inputRefs = "a,b",
            receituarioNumber = "REC-DEMO-001",
            activeIngredient = "GLYPHOSATE",
            moaGroup = "G",
            responsibleTechCpf = "00000000191",
            phiDays = 7,
            deforestationCutoffDate = DEFORESTATION_CUTOFF,
            embargoed = false,
            carStatus = "ATIVO",
        )
        every { evidence.findById("LOT-BV-001") } returns Optional.of(pack)
        val dto = svc.getEvidencePack(AccessScope(DemoTenant.ID, setOf(farmId), "OPERATOR"), "LOT-BV-001")
        assertEquals("LOT-BV-001", dto.lotCode)
        assertTrue(dto.simulation)
        assertFalse(dto.embargoed)
        assertEquals(listOf("a", "b"), dto.inputRefs)
    }

    @Test
    fun evidencePackNotFound() {
        every { evidence.findById("MISSING") } returns Optional.empty()
        val ex = assertThrows(NotFoundException::class.java) {
            svc.getEvidencePack(AccessScope(DemoTenant.ID, setOf(DemoIds.uuid("farm-001")), "OPERATOR"), "MISSING")
        }
        assertEquals("EVIDENCE_PACK_NOT_FOUND", ex.code)
    }

    @Test
    fun creditDossierHappyPath() {
        val farmId = DemoIds.uuid("farm-001")
        every { dossiers.findById(farmId) } returns Optional.of(
            CreditDossierEntity(
                farmId, "MS-1", "ATIVO", false, DEFORESTATION_CUTOFF, true, true, "ok",
            ),
        )
        val dto = svc.getCreditDossier(AccessScope(DemoTenant.ID, setOf(farmId), "OPERATOR"), farmId)
        assertTrue(dto.simulation)
        assertTrue(dto.deforestationClear)
        assertTrue(dto.zarcCompliant)
    }

    @Test
    fun creditDossierNotFound() {
        val farmId = UUID.randomUUID()
        every { dossiers.findById(farmId) } returns Optional.empty()
        val ex = assertThrows(NotFoundException::class.java) {
            svc.getCreditDossier(AccessScope(DemoTenant.ID, setOf(farmId), "OPERATOR"), farmId)
        }
        assertEquals("CREDIT_DOSSIER_NOT_FOUND", ex.code)
    }

    @Test
    fun nfeHomologationIncludesReceituarioAndCpf() {
        val xml = nfeHomologationXml("REC-DEMO-001", "00000000191")
        assertTrue(xml.contains("<tpAmb>2</tpAmb>"))
        assertTrue(xml.contains("<nReceituario>REC-DEMO-001</nReceituario>"))
        assertTrue(xml.contains("<CPFRespTec>00000000191</CPFRespTec>"))
    }

    @Test
    fun nfeHomologationNullReceituarioYieldsEmptyElement() {
        val xml = nfeHomologationXml(null, "00000000191")
        assertTrue(xml.contains("<nReceituario></nReceituario>"))
        assertTrue(xml.contains("<CPFRespTec>00000000191</CPFRespTec>"))
    }
}
