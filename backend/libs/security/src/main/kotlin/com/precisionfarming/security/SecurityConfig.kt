package com.precisionfarming.security

import com.precisionfarming.common.PemKeys
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtProperties: JwtProperties,
    private val correlationFilter: CorrelationFilter,
    @Value("\${springdoc.api-docs.enabled:false}") private val springdocEnabled: Boolean,
    @Value("\${spring.application.name:}") private val applicationName: String,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtDecoder(): JwtDecoder {
        require(jwtProperties.jwtPublicKey.isNotBlank()) { "app.security.jwt-public-key is required" }
        val nimbus = NimbusJwtDecoder.withPublicKey(PemKeys.parsePublic(jwtProperties.jwtPublicKey)).build()
        return JwtDecoder { token ->
            val jwt = nimbus.decode(token)
            JwtAccessType.requireResourceToken(jwt.getClaimAsString(JwtAccessType.CLAIM))
            jwt
        }
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val granted = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("role")
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(granted)
        }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                ).permitAll()
                if (applicationName == "auth-service") {
                    it.requestMatchers(
                        "/internal/service-tokens",
                        "/internal/memberships",
                        "/internal/memberships/revoke",
                    ).permitAll()
                }
                if (springdocEnabled) {
                    it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                }
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) }
            }
            .addFilterBefore(correlationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsSource(): UrlBasedCorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = LocalCors.ORIGINS
            allowedMethods = listOf("*")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
