package com.precisionfarming.security

import com.precisionfarming.common.DomainException
import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.ServiceUnavailableException
import com.precisionfarming.common.UnauthorizedException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

object UpstreamErrorMapper {
    fun map(ex: Exception, notFoundCode: String, notFoundMessage: String): Nothing {
        when (ex) {
            is DomainException -> throw ex
            is RestClientResponseException -> when (ex.statusCode.value()) {
                401 -> throw UnauthorizedException()
                403, 404 -> throw NotFoundException(notFoundCode, notFoundMessage)
                else -> throw ServiceUnavailableException()
            }
            is ResourceAccessException -> throw ServiceUnavailableException()
            else -> throw ServiceUnavailableException()
        }
    }

    fun missingBody(): Nothing = throw ServiceUnavailableException()

    fun requireSameFarm(actual: UUID, expected: UUID, message: String) {
        if (actual != expected) {
            throw ForbiddenException(message, "FARM_SCOPE_DENIED")
        }
    }
}
