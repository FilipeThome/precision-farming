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
