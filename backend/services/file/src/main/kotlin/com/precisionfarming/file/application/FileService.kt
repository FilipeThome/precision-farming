package com.precisionfarming.file.application

import com.precisionfarming.common.DemoIds
import com.precisionfarming.file.infrastructure.FileJpaRepository
import com.precisionfarming.file.infrastructure.FileMetaEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class FileService(private val repo: FileJpaRepository) {
    fun list() = repo.findAll()
    @Transactional
    fun seed() {
        if (repo.existsById(DemoIds.uuid("ndvi-001"))) return
        repo.save(
            FileMetaEntity(
                DemoIds.uuid("ndvi-001"), DemoIds.uuid("farm-001"), DemoIds.uuid("field-001"),
                "NDVI_DEMO", "Demo NDVI", "demo/ndvi/field-001.json", Instant.now(), "0.1.0", "DEMO",
            ),
        )
    }
}

@Service
class FileSeed(private val svc: FileService, @Value("\${app.seed:true}") private val seed: Boolean) {
    @Bean fun seedFiles() = ApplicationRunner { if (seed) svc.seed() }
}
