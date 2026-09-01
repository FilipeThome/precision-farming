package com.precisionfarming.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import javax.crypto.spec.SecretKeySpec

@Configuration
class GatewaySecurityConfig(
    @Value("\${app.security.jwt-secret}") private val jwtSecret: String,
) {
    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val key = SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256")
        return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .authorizeExchange {
                it.pathMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                ).permitAll()
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.anyExchange().authenticated()
            }
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }
            .build()
    }
}
