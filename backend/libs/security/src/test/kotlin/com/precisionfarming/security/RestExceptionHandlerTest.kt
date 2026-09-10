package com.precisionfarming.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.util.UUID

class RestExceptionHandlerTest {
    private val handler = RestExceptionHandler()

    @Test
    fun missingResourceIsNotFound() {
        val req = MockHttpServletRequest()
        val res = handler.missingResource(NoResourceFoundException(HttpMethod.GET, "api/v1/machines/x/metrics"), req)
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
            null,
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
            java.time.Instant::class.java,
            "from",
            null,
            IllegalArgumentException("Invalid instant"),
        )
        val res = handler.typeMismatch(ex, req)
        assertEquals(400, res.statusCode.value())
        assertEquals("VALIDATION_ERROR", res.body!!.code)
    }
}
