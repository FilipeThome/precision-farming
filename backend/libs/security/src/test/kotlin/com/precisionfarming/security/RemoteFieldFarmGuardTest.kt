package com.precisionfarming.security

import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.ServiceUnavailableException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.UUID

class RemoteFieldFarmGuardTest {
    private val farmUrl = "http://farm.example"
    private val fieldId = UUID.randomUUID()
    private val farmId = UUID.randomUUID()

    @Test
    fun mismatchIsFarmScopeDenied() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteFieldFarmGuard(farmUrl, http)
        val other = UUID.randomUUID()
        server.expect(requestTo("$farmUrl/api/v1/fields/$fieldId"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer test"))
            .andRespond(withSuccess("""{"farmId":"$other"}""", MediaType.APPLICATION_JSON))
        GuardHttp.withBearer {
            val ex = assertThrows(ForbiddenException::class.java) {
                guard.requireBelongsToFarm(fieldId, farmId)
            }
            assertEquals("FARM_SCOPE_DENIED", ex.code)
        }
        server.verify()
    }

    @Test
    fun missingFieldIsNotFound() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteFieldFarmGuard(farmUrl, http)
        server.expect(requestTo("$farmUrl/api/v1/fields/$fieldId"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))
        GuardHttp.withBearer {
            val ex = assertThrows(NotFoundException::class.java) {
                guard.requireBelongsToFarm(fieldId, farmId)
            }
            assertEquals("FIELD_NOT_FOUND", ex.code)
        }
        server.verify()
    }

    @Test
    fun upstreamFailureIsUnavailable() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteFieldFarmGuard(farmUrl, http)
        server.expect(requestTo("$farmUrl/api/v1/fields/$fieldId"))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY))
        GuardHttp.withBearer {
            val ex = assertThrows(ServiceUnavailableException::class.java) {
                guard.requireBelongsToFarm(fieldId, farmId)
            }
            assertEquals("UPSTREAM_UNAVAILABLE", ex.code)
        }
        server.verify()
    }

    @Test
    fun fetchDoesNotWriteDemoFieldFarms() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteFieldFarmGuard(farmUrl, http)
        server.expect(requestTo("$farmUrl/api/v1/fields/$fieldId"))
            .andRespond(withSuccess("""{"farmId":"$farmId"}""", MediaType.APPLICATION_JSON))
        GuardHttp.withBearer {
            guard.requireBelongsToFarm(fieldId, farmId)
        }
        assertNull(DemoFieldFarms.farmId(fieldId))
        server.verify()
    }
}
