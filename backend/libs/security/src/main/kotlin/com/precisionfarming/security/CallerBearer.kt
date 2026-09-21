package com.precisionfarming.security

import com.precisionfarming.common.UnauthorizedException
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Duration

object CallerBearer {
    fun header(): String {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: throw UnauthorizedException("Missing bearer token")
        return request.getHeader("Authorization") ?: throw UnauthorizedException("Missing bearer token")
    }
}

object TimedRestClient {
    fun create(): RestClient = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(3))
                setReadTimeout(Duration.ofSeconds(10))
            },
        )
        .build()
}
