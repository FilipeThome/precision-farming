package com.precisionfarming.security

import com.precisionfarming.common.ForbiddenException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.ServiceUnavailableException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.UUID

class RemoteItemFarmGuardTest {
    private val inventoryUrl = "http://inventory.example"
    private val itemId = UUID.randomUUID()
    private val farmId = UUID.randomUUID()

    @Test
    fun cadastroItemBelongsWhenHttpReturnsFarm() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteItemFarmGuard(inventoryUrl, http)
        server.expect(requestTo("$inventoryUrl/api/v1/inventory/$itemId"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""{"farmId":"$farmId"}""", MediaType.APPLICATION_JSON))
        GuardHttp.withBearer {
            assertDoesNotThrow { guard.requireBelongsToFarm(itemId, farmId) }
        }
        server.verify()
    }

    @Test
    fun mismatchIsFarmScopeDenied() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteItemFarmGuard(inventoryUrl, http)
        val other = UUID.randomUUID()
        server.expect(requestTo("$inventoryUrl/api/v1/inventory/$itemId"))
            .andRespond(withSuccess("""{"farmId":"$other"}""", MediaType.APPLICATION_JSON))
        GuardHttp.withBearer {
            val ex = assertThrows(ForbiddenException::class.java) {
                guard.requireBelongsToFarm(itemId, farmId)
            }
            assertEquals("FARM_SCOPE_DENIED", ex.code)
        }
        server.verify()
    }

    @Test
    fun missingItemIsNotFound() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteItemFarmGuard(inventoryUrl, http)
        server.expect(requestTo("$inventoryUrl/api/v1/inventory/$itemId"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))
        GuardHttp.withBearer {
            val ex = assertThrows(NotFoundException::class.java) {
                guard.requireBelongsToFarm(itemId, farmId)
            }
            assertEquals("ITEM_NOT_FOUND", ex.code)
        }
        server.verify()
    }

    @Test
    fun upstreamFailureIsUnavailable() {
        val (server, http) = GuardHttp.client()
        val guard = RemoteItemFarmGuard(inventoryUrl, http)
        server.expect(requestTo("$inventoryUrl/api/v1/inventory/$itemId"))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY))
        GuardHttp.withBearer {
            val ex = assertThrows(ServiceUnavailableException::class.java) {
                guard.requireBelongsToFarm(itemId, farmId)
            }
            assertEquals("UPSTREAM_UNAVAILABLE", ex.code)
        }
        server.verify()
    }
}

