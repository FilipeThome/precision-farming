package com.precisionfarming.weather.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.precisionfarming.weather.domain.MapaZarcLookup
import com.precisionfarming.weather.domain.MonthDayWindow
import com.precisionfarming.weather.domain.parseMapaZarcWindow
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.util.concurrent.TimeUnit

class HttpMapaZarcLookup(
    private val datastoreUrl: String,
    private val timeoutMs: Int,
    private val httpFactory: (Int) -> RestClient = { budgetMs ->
        RestClient.builder()
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    val capped = budgetMs.coerceIn(1, 3000)
                    setConnectTimeout(Duration.ofMillis(capped.toLong()))
                    setReadTimeout(Duration.ofMillis(capped.toLong()))
                },
            )
            .build()
    },
    private val maxPages: Int = MAX_PAGES,
    private val pageSize: Int = PAGE_SIZE,
) : MapaZarcLookup {

    private val budgetMs = timeoutMs.coerceIn(1, 3000)

    override fun window(municipality: String, crop: String): MonthDayWindow? {
        return try {
            val deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(budgetMs.toLong())
            val records = JsonNodeFactory.instance.arrayNode()
            var offset = 0
            var complete = false
            for (pageIndex in 0 until maxPages) {
                val remainingMs = remainingMs(deadlineNs)
                if (remainingMs <= 0) break
                val uri = UriComponentsBuilder.fromUriString(datastoreUrl)
                    .apply {
                        if (datastoreUrl.contains("datastore_search")) {
                            queryParam("resource_id", DEFAULT_RESOURCE_ID)
                        }
                        if (municipality.isNotBlank()) queryParam("q", municipality)
                        queryParam("limit", pageSize)
                        queryParam("offset", offset)
                    }
                    .encode()
                    .build()
                    .toUri()
                val body = httpFactory(remainingMs).get().uri(uri).retrieve().body(String::class.java) ?: break
                val page = ObjectMapper().readTree(body).path("result").path("records")
                if (!page.isArray) break
                if (page.isEmpty) {
                    complete = true
                    break
                }
                page.forEach { records.add(it) }
                if (page.size() < pageSize) {
                    complete = true
                    break
                }
                offset += pageSize
                if (remainingMs(deadlineNs) <= 0) break
            }
            // Partial fetch (timeout / page cap / null body mid-stream) must not look "live".
            if (!complete || records.isEmpty()) return null
            parseMapaZarcWindow(wrapped(records), crop, municipality)
        } catch (_: Exception) {
            null
        }
    }

    private fun remainingMs(deadlineNs: Long): Int {
        val left = TimeUnit.NANOSECONDS.toMillis(deadlineNs - System.nanoTime())
        return left.coerceAtMost(budgetMs.toLong()).toInt().coerceAtLeast(0)
    }

    private fun wrapped(records: ArrayNode): String =
        """{"success":true,"result":{"records":$records}}"""

    companion object {
        const val DEFAULT_RESOURCE_ID = "a8875ff8-fe4d-4c3c-b1a1-3b19c32916f1"
        private const val PAGE_SIZE = 1000
        private const val MAX_PAGES = 5
    }
}

@Configuration
class MapaZarcConfig {

    @Bean
    @ConditionalOnProperty(prefix = "weather.mapa", name = ["live"], havingValue = "true")
    fun liveMapaZarcLookup(
        @Value("\${weather.mapa.datastore-url:https://dados.agricultura.gov.br/api/3/action/datastore_search}")
        datastoreUrl: String,
        @Value("\${weather.mapa.timeout-ms:2500}") timeoutMs: Int,
    ): MapaZarcLookup = HttpMapaZarcLookup(datastoreUrl, timeoutMs)

    @Bean
    @ConditionalOnProperty(
        prefix = "weather.mapa",
        name = ["live"],
        havingValue = "false",
        matchIfMissing = true,
    )
    fun noOpMapaZarcLookup(): MapaZarcLookup = MapaZarcLookup { _, _ -> null }
}
