package com.precisionfarming.auth.application

import java.net.InetAddress

/** Loopback or RFC1918 only — never honor X-Forwarded-For. */
object InternalNets {
    fun allowed(remoteAddr: String?): Boolean {
        if (remoteAddr.isNullOrBlank()) return false
        return try {
            val addr = InetAddress.getByName(remoteAddr.trim())
            addr.isLoopbackAddress || addr.isSiteLocalAddress
        } catch (_: Exception) {
            false
        }
    }
}
