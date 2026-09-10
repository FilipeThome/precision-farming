package com.precisionfarming.auth.application

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InternalNetsTest {
    @Test
    fun allowsLoopbackAndSiteLocal() {
        assertTrue(InternalNets.allowed("127.0.0.1"))
        assertTrue(InternalNets.allowed("::1"))
        assertTrue(InternalNets.allowed("10.0.0.8"))
        assertTrue(InternalNets.allowed("172.18.0.4"))
        assertTrue(InternalNets.allowed("192.168.1.10"))
    }

    @Test
    fun deniesPublicGarbageAndHostnames() {
        assertFalse(InternalNets.allowed(null))
        assertFalse(InternalNets.allowed(""))
        assertFalse(InternalNets.allowed("8.8.8.8"))
        assertFalse(InternalNets.allowed("not-an-ip"))
        assertFalse(InternalNets.allowed("localhost"))
        assertFalse(InternalNets.allowed("evil.example"))
        assertFalse(InternalNets.allowed("08.1.1.1"))
    }
}
