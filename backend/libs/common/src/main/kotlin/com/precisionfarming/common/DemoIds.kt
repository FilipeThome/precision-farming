package com.precisionfarming.common

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object DemoIds {
    private val cache = ConcurrentHashMap<String, UUID>(64)

    fun uuid(name: String): UUID = cache.computeIfAbsent(name) {
        UUID.nameUUIDFromBytes("precision-farming:$name".toByteArray())
    }
}
