package com.precisionfarming.security

import com.precisionfarming.common.ApiError
import com.precisionfarming.common.Correlation
import com.precisionfarming.common.DomainException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class RestExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)
    @ExceptionHandler(DomainException::class)
    fun domain(ex: DomainException, request: HttpServletRequest): ResponseEntity<ApiError> {
        if (ex.httpStatus == 403 || ex.httpStatus == 401) {
            log.warn(
                "authz_deny code={} status={} path={} method={} msg={}",
                ex.code,
                ex.httpStatus,
                request.requestURI,
                request.method,
                ex.message,
            )
        }
        val body = ApiError(
            status = ex.httpStatus,
            code = ex.code,
            message = ex.message ?: ex.code,
            correlationId = request.getHeader(Correlation.HEADER) ?: MDC.get("correlationId"),
            details = ex.details,
        )
        return ResponseEntity.status(ex.httpStatus).body(body)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDenied(ex: AccessDeniedException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn(
            "authz_deny code=FORBIDDEN status=403 path={} method={} msg={}",
            request.requestURI,
            request.method,
            ex.message,
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiError(
                status = 403,
                code = "FORBIDDEN",
                message = ex.message ?: "Forbidden",
                correlationId = request.getHeader(Correlation.HEADER) ?: MDC.get("correlationId"),
            ),
        )
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

    @ExceptionHandler(NoResourceFoundException::class)
    fun missingResource(ex: NoResourceFoundException, request: HttpServletRequest): ResponseEntity<ApiError> =
        notFound(request, "NOT_FOUND", "Not found")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun typeMismatch(ex: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<ApiError> {
        if (isPathVariable(ex)) return notFound(request, "NOT_FOUND", "Not found")
        return ResponseEntity.badRequest().body(
            ApiError(
                status = 400,
                code = "VALIDATION_ERROR",
                message = "Invalid ${ex.name}",
                correlationId = request.getHeader(Correlation.HEADER) ?: MDC.get("correlationId"),
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun other(ex: Exception): ResponseEntity<ApiError> {
        log.error("Unhandled error", ex)
        return ResponseEntity.status(500).body(
            ApiError(
                status = 500,
                code = "INTERNAL_ERROR",
                message = "Unexpected error",
                correlationId = MDC.get("correlationId"),
            ),
        )
    }

    private fun isPathVariable(ex: MethodArgumentTypeMismatchException): Boolean =
        ex.parameter.hasParameterAnnotation(PathVariable::class.java)

    private fun notFound(request: HttpServletRequest, code: String, message: String): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(
                status = 404,
                code = code,
                message = message,
                correlationId = request.getHeader(Correlation.HEADER) ?: MDC.get("correlationId"),
            ),
        )
}
