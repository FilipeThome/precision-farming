package com.precisionfarming.sync.api

import com.precisionfarming.security.FarmAccess
import com.precisionfarming.sync.application.SyncService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class SyncCommandDto(val clientOperationId: String, val type: String, val createdAt: Instant, val payload: Map<String, Any?> = emptyMap())
data class PushRequest(val deviceId: String, val commands: List<SyncCommandDto>)
data class PullRequest(val deviceId: String, val cursor: String?)

@RestController
@RequestMapping("/api/v1/sync")
class SyncController(
    private val svc: SyncService,
    private val farmAccess: FarmAccess,
) {
    @PostMapping("/push")
    fun push(@RequestBody body: PushRequest) = svc.push(farmAccess.current(), body)

    @PostMapping("/pull")
    fun pull(@RequestBody body: PullRequest) = svc.pull(farmAccess.current(), body.deviceId, body.cursor)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class SyncSeedController {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "sync")
}
