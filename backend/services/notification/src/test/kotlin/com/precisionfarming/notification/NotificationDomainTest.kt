package com.precisionfarming.notification

import com.precisionfarming.common.DemoIds
import com.precisionfarming.notification.application.NotificationService
import com.precisionfarming.notification.infrastructure.NotificationEntity
import com.precisionfarming.notification.infrastructure.NotificationJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class NotificationDomainTest {
    private val repo = mockk<NotificationJpaRepository>()
    private val svc = NotificationService(repo)

    @Test
    fun listReturnsDtosForUser() {
        val user = DemoIds.uuid("manager@precisionfarming.demo")
        val row = NotificationEntity(UUID.randomUUID(), user, "ALERT", "t", "b", null, Instant.now())
        every { repo.findByUserId(user) } returns listOf(row)
        val result = svc.list(user)
        assertEquals(1, result.size)
        assertEquals(user, result[0].userId)
        assertEquals("ALERT", result[0].type)
    }

    @Test
    fun listIsEmptyForOtherUser() {
        every { repo.findByUserId(DemoIds.uuid("operator@precisionfarming.demo")) } returns emptyList()
        assertTrue(svc.list(DemoIds.uuid("operator@precisionfarming.demo")).isEmpty())
    }
}
