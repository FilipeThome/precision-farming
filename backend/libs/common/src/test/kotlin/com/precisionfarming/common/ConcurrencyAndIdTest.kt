package com.precisionfarming.common

import com.precisionfarming.common.DemoCatalog
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.QueryLimits
import com.precisionfarming.common.concurrency.VirtualJobs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

class ConcurrencyAndIdTest {
    @Test
    fun demoIdsAreCachedAndMatchWebFarm001() {
        val a = DemoIds.uuid("farm-001")
        val b = DemoIds.uuid("farm-001")
        assertSame(a, b)
        assertEquals(UUID.fromString("bbc017bc-be38-34d4-95df-0b1f15162e1d"), a)
        assertEquals(UUID.fromString("9e876222-3f39-3190-ad8c-1c147b28f6e6"), DemoIds.uuid("field-015"))
        assertEquals(UUID.fromString("9861d50d-527b-385c-b89b-b0674467015f"), DemoIds.uuid("machine-013"))
        assertEquals(UUID.fromString("9b2296fa-d133-37db-be2f-be69dc802915"), DemoIds.uuid("machine-014"))
    }

    @Test
    fun demoCatalogResolvesFarmAndFieldNames() {
        val farmId = DemoIds.uuid("farm-001")
        assertEquals("Fazenda Boa Vista", DemoCatalog.farmName(farmId))
        assertEquals("Talhão 01", DemoCatalog.fieldName(DemoIds.uuid("field-001")))
        assertEquals("Trator 01", DemoCatalog.machineName(DemoIds.uuid("machine-001")))
        assertEquals("Drone 01", DemoCatalog.machineName(DemoIds.uuid("machine-013")))
        assertEquals("Drone 02", DemoCatalog.machineName(DemoIds.uuid("machine-014")))
        assertEquals("Talhão Leste", DemoCatalog.fieldName(DemoIds.uuid("field-009")))
        assertEquals("Talhão Nordeste", DemoCatalog.fieldName(DemoIds.uuid("field-015")))
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

    @Test
    fun cappedTruncatesAboveMaxList() {
        val items = (1..QueryLimits.MAX_LIST + 10).toList()
        assertEquals(QueryLimits.MAX_LIST, items.capped().size)
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).capped())
    }
}
