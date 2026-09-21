package com.precisionfarming.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.Instant
import java.util.UUID

class RestExceptionHandlerTest {
    private val handler = RestExceptionHandler()

    @Suppress("unused")
    fun pathId(@PathVariable id: UUID) {}

    @Suppress("unused")
    fun queryFrom(@RequestParam from: Instant) {}

    @Test
    fun missingResourceIsNotFound() {
        val req = MockHttpServletRequest()
        val res = handler.missingResource(
            NoResourceFoundException(HttpMethod.GET, "", "api/v1/machines/x/metrics"),
            req,
        )
        assertEquals(404, res.statusCode.value())
        assertEquals("NOT_FOUND", res.body!!.code)
        assertEquals("Not found", res.body!!.message)
    }

    @Test
    fun pathIdMismatchIsNotFound() {
        val req = MockHttpServletRequest()
        val ex = org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
            "not-a-uuid",
            UUID::class.java,
            "id",
            MethodParameter(javaClass.getDeclaredMethod("pathId", UUID::class.java), 0),
            IllegalArgumentException("Invalid UUID string: not-a-uuid"),
        )
        val res = handler.typeMismatch(ex, req)
        assertEquals(404, res.statusCode.value())
        assertEquals("NOT_FOUND", res.body!!.code)
    }

    @Test
    fun queryParamMismatchIsValidationError() {
        val req = MockHttpServletRequest()
        val ex = org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
            "yesterday",
            Instant::class.java,
            "from",
            MethodParameter(javaClass.getDeclaredMethod("queryFrom", Instant::class.java), 0),
            IllegalArgumentException("Invalid instant"),
        )
        val res = handler.typeMismatch(ex, req)
        assertEquals(400, res.statusCode.value())
        assertEquals("VALIDATION_ERROR", res.body!!.code)
    }

    @Test
    fun serviceUnavailableIsMapped() {
        val req = MockHttpServletRequest()
        val res = handler.domain(com.precisionfarming.common.ServiceUnavailableException(), req)
        assertEquals(503, res.statusCode.value())
        assertEquals("UPSTREAM_UNAVAILABLE", res.body!!.code)
    }
}
