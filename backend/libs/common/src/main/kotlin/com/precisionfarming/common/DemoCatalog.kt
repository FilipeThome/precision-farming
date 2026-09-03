package com.precisionfarming.common

import java.util.UUID

/** Stable demo display names keyed by [DemoIds] seed keys. */
object DemoCatalog {
    val farms: Map<String, String> = mapOf(
        "farm-001" to "Fazenda Boa Vista",
        "farm-002" to "Fazenda Santa Helena",
        "farm-003" to "Fazenda Horizonte",
        "farm-004" to "Fazenda Primavera",
        "farm-005" to "Fazenda Campo Alegre",
        "farm-006" to "Fazenda Vale Verde",
        "farm-007" to "Fazenda Estrela do Sul",
        "farm-008" to "Fazenda Nova Esperança",
    )

    val fields: Map<String, String> = mapOf(
        "field-001" to "Talhão 01",
        "field-002" to "Talhão 02",
        "field-003" to "Talhão 03",
        "field-004" to "Talhão Norte",
        "field-005" to "Talhão Sul",
        "field-006" to "Talhão A",
        "field-007" to "Talhão B",
        "field-008" to "Talhão C",
        "field-009" to "Talhão Leste",
        "field-010" to "Talhão Oeste",
        "field-011" to "Talhão Centro",
        "field-012" to "Talhão 1",
        "field-013" to "Talhão 2",
        "field-014" to "Talhão 04",
        "field-015" to "Talhão Nordeste",
        "field-016" to "Talhão 3",
        "field-017" to "Talhão VV-01",
        "field-018" to "Talhão VV-02",
        "field-019" to "Talhão ES-Norte",
        "field-020" to "Talhão ES-Sul",
        "field-021" to "Talhão NE-01",
        "field-022" to "Talhão NE-02",
    )

    val machines: Map<String, String> = mapOf(
        "machine-001" to "Trator 01",
        "machine-002" to "Pulverizador 01",
        "machine-003" to "Colheitadeira 01",
        "machine-004" to "Trator 02",
        "machine-005" to "Plantadeira 01",
        "machine-006" to "Pulverizador 02",
        "machine-007" to "Trator 03",
        "machine-008" to "Colheitadeira 02",
        "machine-009" to "Trator 04",
        "machine-010" to "Plantadeira 02",
        "machine-011" to "Pulverizador 03",
        "machine-012" to "Trator 05",
        "machine-013" to "Drone 01",
        "machine-014" to "Drone 02",
    )

    fun farmName(id: UUID?): String? = id?.let { farmsById[it] }
    fun fieldName(id: UUID?): String? = id?.let { fieldsById[it] }
    fun machineName(id: UUID?): String? = id?.let { machinesById[it] }

    private val farmsById: Map<UUID, String> by lazy { farms.mapKeys { DemoIds.uuid(it.key) } }
    private val fieldsById: Map<UUID, String> by lazy { fields.mapKeys { DemoIds.uuid(it.key) } }
    private val machinesById: Map<UUID, String> by lazy { machines.mapKeys { DemoIds.uuid(it.key) } }
}
