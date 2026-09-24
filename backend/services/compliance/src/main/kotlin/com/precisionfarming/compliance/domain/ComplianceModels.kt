package com.precisionfarming.compliance.domain

import java.math.BigDecimal
import java.time.LocalDate

data class EsgScorecard(val overall: BigDecimal, val co2ePerHa: BigDecimal, val waterPerTon: BigDecimal)

val DEFORESTATION_CUTOFF: LocalDate = LocalDate.of(2020, 12, 31)

/**
 * Homologation-only NF-e agro stub (`tpAmb=2`). Not signed, not transmitted to SEFAZ.
 * Empty element text when receituario / CPF is null or blank.
 */
fun nfeHomologationXml(receituarioNumber: String?, responsibleTechCpf: String?): String {
    val nReceituario = receituarioNumber?.takeIf { it.isNotBlank() }.orEmpty()
    val cpf = responsibleTechCpf?.takeIf { it.isNotBlank() }.orEmpty()
    return buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append("<NFe><infNFe><ide><tpAmb>2</tpAmb></ide>")
        append("<agropecuario>")
        append("<nReceituario>").append(escapeXml(nReceituario)).append("</nReceituario>")
        append("<CPFRespTec>").append(escapeXml(cpf)).append("</CPFRespTec>")
        append("</agropecuario></infNFe></NFe>")
    }
}

private fun escapeXml(value: String): String = buildString(value.length) {
    for (ch in value) {
        when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(ch)
        }
    }
}
