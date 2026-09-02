package com.precisionfarming.common

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object PemKeys {
    const val DEMO_PUBLIC_FINGERPRINT = "96aa3b19d9d5fe43f5369df49cb39849897b3855c9d9a99e2413641b224ee68e"

    fun parsePublic(pem: String): RSAPublicKey {
        val spec = X509EncodedKeySpec(decode(pem, "PUBLIC KEY"))
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }

    fun parsePrivate(pem: String): RSAPrivateKey {
        val spec = PKCS8EncodedKeySpec(decode(pem, "PRIVATE KEY"))
        return KeyFactory.getInstance("RSA").generatePrivate(spec) as RSAPrivateKey
    }

    fun publicFingerprint(pem: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(decode(pem, "PUBLIC KEY"))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isDemoPublicKey(pem: String): Boolean =
        pem.isNotBlank() && publicFingerprint(pem) == DEMO_PUBLIC_FINGERPRINT

    fun secretsEqual(left: String, right: String): Boolean {
        val a = left.toByteArray(Charsets.UTF_8)
        val b = right.toByteArray(Charsets.UTF_8)
        if (a.size != b.size) {
            MessageDigest.isEqual(a, a)
            return false
        }
        return MessageDigest.isEqual(a, b)
    }

    private fun decode(pem: String, type: String): ByteArray {
        val normalized = pem.replace("\\n", "\n").trim()
        val body = normalized
            .replace("-----BEGIN $type-----", "")
            .replace("-----END $type-----", "")
            .replace("\\s".toRegex(), "")
        require(body.isNotBlank()) { "empty $type PEM" }
        return Base64.getDecoder().decode(body)
    }
}
