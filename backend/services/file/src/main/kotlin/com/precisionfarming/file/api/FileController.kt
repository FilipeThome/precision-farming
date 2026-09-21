package com.precisionfarming.file.api

import com.precisionfarming.file.application.FileService
import com.precisionfarming.security.FarmAccess
import org.springframework.core.io.PathResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val svc: FileService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping
    fun list() = svc.list(farmAccess.current())

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = svc.get(farmAccess.current(), id)

    @GetMapping("/{id}/content")
    fun content(@PathVariable id: UUID): ResponseEntity<PathResource> {
        val payload = svc.content(farmAccess.current(), id)
        val filename = when {
            payload.contentType.contains("png") -> "machine.png"
            payload.contentType.contains("webp") -> "machine.webp"
            else -> "machine.jpg"
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(payload.contentType))
            .contentLength(payload.sizeBytes)
            .cacheControl(CacheControl.empty().cachePrivate())
            .header(HttpHeaders.CACHE_CONTROL, "private")
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$filename\"")
            .body(PathResource(payload.path))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER','MAINTENANCE')")
    fun upload(
        @RequestParam farmId: UUID,
        @RequestParam kind: String,
        @RequestParam(required = false) entityId: UUID?,
        @RequestParam file: MultipartFile,
    ) = svc.upload(farmAccess.current(), farmId, kind, entityId, file.bytes)
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
