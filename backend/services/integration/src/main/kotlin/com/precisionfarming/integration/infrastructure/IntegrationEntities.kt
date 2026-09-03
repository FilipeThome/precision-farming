package com.precisionfarming.integration.infrastructure

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "connectors")
class ConnectorEntity(
    @Id val id: UUID,
    val name: String,
    val type: String,
    val mode: String,
    val capabilities: String,
)

interface ConnectorJpaRepository : JpaRepository<ConnectorEntity, UUID>
