package com.precisionfarming.asset

import com.precisionfarming.asset.infrastructure.RemoteMachinePhotoGuard
import com.precisionfarming.common.ConflictException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.ServiceUnavailableException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

class RemoteMachinePhotoGuardTest {
    private val fileUrl = "http://file.example"
    private val fileId = UUID.randomUUID()
    private val farmId = UUID.randomUUID()
    private val machineId = UUID.randomUUID()

    @Test
    fun mismatchIsFarmScopeDenied() {
        val (server, guard) = guard()
        server.expect(requestTo("$fileUrl/api/v1/files/$fileId"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"farmId":"${UUID.randomUUID()}","kind":"MACHINE_PHOTO","entityId":null}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
        withBearer {
            val ex = assertThrows(com.precisionfarming.common.ForbiddenException::class.java) {
                guard.requireAssignable(fileId, farmId, machineId)
            }
            assertEquals("FARM_SCOPE_DENIED", ex.code)
        }
        server.verify()
    }

    @Test
    fun missingFileIsNotFound() {
        val (server, guard) = guard()
        server.expect(requestTo("$fileUrl/api/v1/files/$fileId"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))
        withBearer {
            val ex = assertThrows(NotFoundException::class.java) {
                guard.requireAssignable(fileId, farmId, machineId)
            }
            assertEquals("FILE_NOT_FOUND", ex.code)
        }
        server.verify()
    }

    @Test
    fun upstreamFailureIsUnavailable() {
        val (server, guard) = guard()
        server.expect(requestTo("$fileUrl/api/v1/files/$fileId"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))
        withBearer {
            val ex = assertThrows(ServiceUnavailableException::class.java) {
                guard.requireAssignable(fileId, farmId, machineId)
            }
            assertEquals("UPSTREAM_UNAVAILABLE", ex.code)
        }
        server.verify()
    }

    @Test
    fun alreadyBoundToAnotherMachineIsConflict() {
        val (server, guard) = guard()
        val other = UUID.randomUUID()
        server.expect(requestTo("$fileUrl/api/v1/files/$fileId"))
            .andRespond(
                withSuccess(
                    """{"farmId":"$farmId","kind":"MACHINE_PHOTO","entityId":"$other"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
        withBearer {
            val ex = assertThrows(ConflictException::class.java) {
                guard.requireAssignable(fileId, farmId, machineId)
            }
            assertEquals("FILE_ALREADY_BOUND", ex.code)
        }
        server.verify()
    }

    private fun guard(): Pair<MockRestServiceServer, RemoteMachinePhotoGuard> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        return server to RemoteMachinePhotoGuard(fileUrl, builder.build())
    }

    private fun withBearer(block: () -> Unit) {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer test")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        try {
            block()
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }
}
