package com.precisionfarming.auth

import com.precisionfarming.security.JwtProperties
import com.precisionfarming.security.issuer.JwtIssuer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthJwtConfig {
    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = ["jwt-private-key"])
    fun jwtIssuer(props: JwtProperties): JwtIssuer {
        require(props.jwtPrivateKey.isNotBlank()) { "auth-service requires app.security.jwt-private-key" }
        require(props.jwtPublicKey.isNotBlank()) { "auth-service requires app.security.jwt-public-key" }
        require(props.serviceMintSecret.length >= 32) { "auth-service requires a 32+ char service mint secret" }
        require(props.authInternalSecret.length >= 32) { "auth-service requires a 32+ char internal secret" }
        return JwtIssuer(props)
    }
}
