package com.precisionfarming.common

import com.precisionfarming.common.concurrency.VirtualJobs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

class ConcurrencyAndIdTest {
    @Test
    fun demoIdsAreCachedAndStable() {
        val a = DemoIds.uuid("farm-001")
        val b = DemoIds.uuid("farm-001")
        assertSame(a, b)
        assertEquals(a, DemoIds.uuid("farm-001"))
    }

    @Test
    fun virtualJobsRunInParallelAndPreserveOrder() {
        val seen = ConcurrentHashMap.newKeySet<String>()
        val result = VirtualJobs.all(
            listOf(
                Callable {
                    seen.add("a")
                    1
                },
                Callable {
                    seen.add("b")
                    2
                },
            ),
        )
        assertEquals(listOf(1, 2), result)
        assertEquals(setOf("a", "b"), seen)
    }
}
