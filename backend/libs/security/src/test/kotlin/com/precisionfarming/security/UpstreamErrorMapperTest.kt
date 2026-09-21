package com.precisionfarming.security

import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.ServiceUnavailableException
import com.precisionfarming.common.UnauthorizedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.util.UUID

class UpstreamErrorMapperTest {
    @Test
    fun unauthorizedStays401() {
        val ex = assertThrows(UnauthorizedException::class.java) {
            UpstreamErrorMapper.map(http(HttpStatus.UNAUTHORIZED), "FIELD_NOT_FOUND", "Field not found")
        }
        assertEquals("UNAUTHORIZED", ex.code)
        assertEquals(401, ex.httpStatus)
    }

    @Test
    fun notFoundKeepsResourceCode() {
        val ex = assertThrows(NotFoundException::class.java) {
            UpstreamErrorMapper.map(http(HttpStatus.NOT_FOUND), "MACHINE_NOT_FOUND", "Machine not found")
        }
        assertEquals("MACHINE_NOT_FOUND", ex.code)
        assertEquals(404, ex.httpStatus)
    }

    @Test
    fun upstreamForbiddenLooksMissing() {
        val ex = assertThrows(NotFoundException::class.java) {
            UpstreamErrorMapper.map(http(HttpStatus.FORBIDDEN), "ITEM_NOT_FOUND", "Item not found")
        }
        assertEquals("ITEM_NOT_FOUND", ex.code)
        assertEquals(404, ex.httpStatus)
    }

    @Test
    fun serverErrorIsUnavailable() {
        val ex = assertThrows(ServiceUnavailableException::class.java) {
            UpstreamErrorMapper.map(HttpServerErrorException(HttpStatus.BAD_GATEWAY), "FIELD_NOT_FOUND", "Field not found")
        }
        assertEquals("UPSTREAM_UNAVAILABLE", ex.code)
        assertEquals(503, ex.httpStatus)
    }

    @Test
    fun timeoutIsUnavailable() {
        val ex = assertThrows(ServiceUnavailableException::class.java) {
            UpstreamErrorMapper.map(ResourceAccessException("timeout"), "FIELD_NOT_FOUND", "Field not found")
        }
        assertEquals("UPSTREAM_UNAVAILABLE", ex.code)
    }

    @Test
    fun farmMismatchIsForbiddenOnly() {
        val actual = UUID.randomUUID()
        val expected = UUID.randomUUID()
        val ex = assertThrows(ForbiddenException::class.java) {
            UpstreamErrorMapper.requireSameFarm(actual, expected, "Field does not belong to farm")
        }
        assertEquals("FARM_SCOPE_DENIED", ex.code)
        assertEquals(403, ex.httpStatus)
    }

    private fun http(status: HttpStatus) = HttpClientErrorException.create(
        status,
        status.reasonPhrase,
        HttpHeaders(),
        ByteArray(0),
        null,
    )
}
