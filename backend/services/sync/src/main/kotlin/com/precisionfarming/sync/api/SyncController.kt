package com.precisionfarming.sync.api

import com.precisionfarming.sync.application.SyncService
import org.springframework.web.bind.annotation.*
import java.time.Instant
import org.springframework.security.access.prepost.PreAuthorize

data class SyncCommandDto(val clientOperationId: String, val type: String, val createdAt: Instant, val payload: Map<String, Any?> = emptyMap())
data class PushRequest(val deviceId: String, val commands: List<SyncCommandDto>)
data class PullRequest(val deviceId: String, val cursor: String?)

@RestController
@RequestMapping("/api/v1/sync")
class SyncController(private val svc: SyncService) {
    @PostMapping("/push")
    fun push(@RequestBody body: PushRequest) = svc.push(body)

    @PostMapping("/pull")
    fun pull(@RequestBody body: PullRequest) = svc.pull(body.deviceId, body.cursor)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class SyncSeedController {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "sync")
}
