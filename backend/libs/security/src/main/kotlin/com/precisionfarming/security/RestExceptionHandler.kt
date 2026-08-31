package com.precisionfarming.security

import com.precisionfarming.common.ApiError
import com.precisionfarming.common.Correlation
import com.precisionfarming.common.DomainException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RestExceptionHandler {
    @ExceptionHandler(DomainException::class)
    fun domain(ex: DomainException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val body = ApiError(
            status = ex.httpStatus,
            code = ex.code,
            message = ex.message ?: ex.code,
            correlationId = request.getHeader(Correlation.HEADER) ?: MDC.get("correlationId"),
            details = ex.details,
        )
        return ResponseEntity.status(ex.httpStatus).body(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(
                status = 400,
                code = "VALIDATION_ERROR",
                message = ex.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" },
                correlationId = MDC.get("correlationId"),
            ),
        )

    @ExceptionHandler(Exception::class)
    fun other(ex: Exception): ResponseEntity<ApiError> =
        ResponseEntity.status(500).body(
            ApiError(
                status = 500,
                code = "INTERNAL_ERROR",
                message = ex.message ?: "Unexpected error",
                correlationId = MDC.get("correlationId"),
            ),
        )
}
