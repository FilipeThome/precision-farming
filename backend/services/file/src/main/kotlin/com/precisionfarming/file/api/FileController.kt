package com.precisionfarming.file.api

import com.precisionfarming.file.application.FileService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/files")
class FileController(private val svc: FileService) {
    @GetMapping fun list() = svc.list()
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class FileSeedController(private val svc: FileService) {
    @PostMapping("/reset") fun reset() = mapOf("status" to "seeded", "service" to "file").also { svc.seed() }
}
