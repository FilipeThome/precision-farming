package com.precisionfarming.sync

import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoTenant
import com.precisionfarming.sync.api.PushRequest
import com.precisionfarming.sync.api.SyncCommandDto
import com.precisionfarming.sync.application.SyncService
import com.precisionfarming.sync.infrastructure.SyncCommandEntity
import com.precisionfarming.sync.infrastructure.SyncJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SyncServiceTest {
    private val repo = mockk<SyncJpaRepository>()
    private val svc = SyncService(repo)

    @Test
    fun rejectsUnboundDeviceId() {
        val userId = UUID.randomUUID()
        val scope = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "OPERATOR", userId)
        assertThrows(ForbiddenException::class.java) {
            svc.push(
                scope,
                PushRequest(
                    deviceId = "other-device",
                    commands = listOf(SyncCommandDto("c1", "PING", Instant.now())),
                ),
            )
        }
    }

    @Test
    fun acceptsDevicePrefixedByUserId() {
        val userId = UUID.randomUUID()
        val scope = AccessScope(DemoTenant.ID, setOf(UUID.randomUUID()), "OPERATOR", userId)
        every { repo.findByClientOperationIdIn(any()) } returns emptyList()
        every { repo.saveAll(any<List<SyncCommandEntity>>()) } answers { firstArg() }
        svc.push(
            scope,
            PushRequest(
                deviceId = "$userId:android-1",
                commands = listOf(SyncCommandDto("c1", "PING", Instant.now())),
            ),
        )
    }
}
