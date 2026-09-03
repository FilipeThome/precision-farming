package com.precisionfarming.security.issuer

import com.precisionfarming.common.DemoIds
import com.precisionfarming.security.JwtAccessType
import com.precisionfarming.security.JwtProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.UUID

class JwtIssuerTest {
    private val issuer = JwtIssuer(testProps())

    @Test
    fun accessTokenRequiresFarmIds() {
        assertThrows(IllegalArgumentException::class.java) {
            issuer.createAccessToken(UUID.randomUUID(), "a@b.c", "ADMIN", emptyList())
        }
    }

    @Test
    fun accessTokenCarriesTypeAndFarms() {
        val farm = DemoIds.uuid("farm-001")
        val user = UUID.randomUUID()
        val token = issuer.createAccessToken(user, "a@b.c", "ADMIN", listOf(farm))
        val claims = issuer.parse(token)
        assertEquals(JwtAccessType.ACCESS, claims["type"])
        assertEquals(user.toString(), claims.subject)
        assertEquals("ADMIN", claims["role"])
    }

    @Test
    fun serviceTokenRejectsEmptyFarms() {
        assertThrows(IllegalArgumentException::class.java) {
            issuer.createServiceToken(UUID.randomUUID(), emptyList())
        }
    }

    @Test
    fun refreshHasJti() {
        val issued = issuer.createRefreshToken(UUID.randomUUID())
        val claims = issuer.parse(issued.token)
        assertEquals(JwtAccessType.REFRESH, claims["type"])
        assertEquals(issued.jti.toString(), claims.id)
    }

    companion object {
        fun testProps(): JwtProperties {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(2048)
            val pair = gen.generateKeyPair()
            val pub = pair.public as RSAPublicKey
            val priv = pair.private as RSAPrivateKey
            return JwtProperties(
                jwtPublicKey = pem("PUBLIC KEY", pub.encoded),
                jwtPrivateKey = pem("PRIVATE KEY", priv.encoded),
                allowDemoSecrets = true,
            )
        }

        private fun pem(type: String, der: ByteArray): String {
            val b64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der)
            return "-----BEGIN $type-----\n$b64\n-----END $type-----\n"
        }
    }
}
