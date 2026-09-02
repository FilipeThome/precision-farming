package com.precisionfarming.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import javax.crypto.spec.SecretKeySpec

@Configuration
class GatewaySecurityConfig(
    @Value("\${app.security.jwt-secret}") private val jwtSecret: String,
) {
    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val key = SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256")
        val nimbus = NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
        return ReactiveJwtDecoder { token ->
            nimbus.decode(token).handle { jwt, sink ->
                val type = jwt.getClaimAsString("type")
                if (!"access".equals(type, ignoreCase = true)) {
                    sink.error(JwtException("Access token required"))
                } else {
                    sink.next(jwt)
                }
            }
        }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("http://localhost:*", "http://127.0.0.1:*")
            allowedMethods = listOf("*")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
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
