package com.precisionfarming.file.api

import com.precisionfarming.file.application.FileService
import com.precisionfarming.security.FarmAccess
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val svc: FileService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping
    fun list() = svc.list(farmAccess.current())
}

@RestController
@RequestMapping("/api/v1/map")
class MapController(
    private val svc: FileService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/layers")
    fun layers(@RequestParam(required = false) farmId: UUID?) = svc.listLayers(farmAccess.current(), farmId)

    @GetMapping("/tiles/{layerId}")
    fun tiles(@PathVariable layerId: UUID) = svc.tileStub(farmAccess.current(), layerId)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class FileSeedController(private val svc: FileService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "file").also { svc.seed() }
}
