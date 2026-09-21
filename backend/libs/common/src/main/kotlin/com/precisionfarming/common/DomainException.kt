package com.precisionfarming.common

open class DomainException(
    val code: String,
    message: String,
    val httpStatus: Int = 400,
    val details: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

class NotFoundException(code: String = "NOT_FOUND", message: String = "Resource not found") :
    DomainException(code, message, 404)

class ConflictException(
    code: String = "CONFLICT",
    message: String,
    details: Map<String, Any?> = emptyMap(),
) : DomainException(code, message, 409, details)

class UnauthorizedException(message: String = "Unauthorized") :
    DomainException("UNAUTHORIZED", message, 401)

class ForbiddenException(message: String = "Forbidden", code: String = "FORBIDDEN") :
    DomainException(code, message, 403)

class TooManyRequestsException(
    message: String = "Too many requests",
    code: String = "RATE_LIMITED",
) : DomainException(code, message, 429)

class ServiceUnavailableException(
    message: String = "Upstream unavailable",
    code: String = "UPSTREAM_UNAVAILABLE",
) : DomainException(code, message, 503)
