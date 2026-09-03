package com.precisionfarming.auth

import com.precisionfarming.security.JwtProperties
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

object RsaTestKeys {
    fun props(): JwtProperties {
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
