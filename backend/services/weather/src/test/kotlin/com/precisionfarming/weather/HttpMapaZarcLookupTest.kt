package com.precisionfarming.weather

import com.precisionfarming.weather.infrastructure.HttpMapaZarcLookup
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class HttpMapaZarcLookupTest {

    @Test
    fun incompletePaginationReturnsNull() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val pageFull = recordsJson(
            (0 until 1000).map { i ->
                """{"cultura":"Soja","mes":10,"decendio":1,"municipio":"São Gabriel do Oeste","UF":"MS","_i":$i}"""
            },
        )
        server.expect(method(HttpMethod.GET))
            .andRespond(withSuccess(pageFull, MediaType.APPLICATION_JSON))

        val client = builder.build()
        val lookup = HttpMapaZarcLookup(
            datastoreUrl = "https://example.test/api/3/action/datastore_search",
            timeoutMs = 2500,
            httpFactory = { client },
            maxPages = 1,
        )
        assertNull(lookup.window("São Gabriel do Oeste - MS", "SOY"))
        server.verify()
    }

    @Test
    fun completeSinglePageParsesWindow() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val page = recordsJson(
            listOf(
                """{"cultura":"Soja","mes":10,"decendio":1,"municipio":"São Gabriel do Oeste","UF":"MS"}""",
                """{"cultura":"Soja","mes":12,"decendio":2,"municipio":"São Gabriel do Oeste","UF":"MS"}""",
            ),
        )
        server.expect(method(HttpMethod.GET))
            .andRespond(withSuccess(page, MediaType.APPLICATION_JSON))

        val client = builder.build()
        val lookup = HttpMapaZarcLookup(
            datastoreUrl = "https://example.test/api/3/action/datastore_search",
            timeoutMs = 2500,
            httpFactory = { client },
        )
        assertNotNull(lookup.window("São Gabriel do Oeste - MS", "SOY"))
        server.verify()
    }

    private fun recordsJson(rows: List<String>): String =
        """{"success":true,"result":{"records":[${rows.joinToString(",")}]}}"""
}
