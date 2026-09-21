package com.precisionfarming.security

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

internal object GuardHttp {
    fun withBearer(token: String = "Bearer test", block: () -> Unit) {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", token)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        try {
            block()
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }

    fun client(): Pair<MockRestServiceServer, RestClient> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        return server to builder.build()
    }
}
