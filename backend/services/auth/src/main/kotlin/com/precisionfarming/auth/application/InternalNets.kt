package com.precisionfarming.auth.application

import java.net.InetAddress

/** Loopback or RFC1918 only — IP literals, never DNS, never X-Forwarded-For. */
object InternalNets {
    fun allowed(remoteAddr: String?): Boolean {
        val addr = parseLiteral(remoteAddr) ?: return false
        return addr.isLoopbackAddress || addr.isSiteLocalAddress
    }

    internal fun parseLiteral(remoteAddr: String?): InetAddress? {
        if (remoteAddr.isNullOrBlank()) return null
        val raw = remoteAddr.trim().substringBefore('%')
        return try {
            if (raw.contains(':')) parseIpv6(raw) else parseIpv4(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseIpv4(raw: String): InetAddress? {
        val parts = raw.split('.')
        if (parts.size != 4) return null
        val bytes = ByteArray(4)
        for (i in parts.indices) {
            val n = parts[i].toIntOrNull() ?: return null
            if (n !in 0..255 || parts[i] != n.toString()) return null
            bytes[i] = n.toByte()
        }
        return InetAddress.getByAddress(bytes)
    }

    private fun parseIpv6(raw: String): InetAddress? {
        if (raw.any { it !in IPV6_CHARS }) return null
        return InetAddress.getByName(raw)
    }

    private const val IPV6_CHARS = "0123456789abcdefABCDEF:"
}
