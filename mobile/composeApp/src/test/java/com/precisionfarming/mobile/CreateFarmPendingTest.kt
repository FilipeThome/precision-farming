package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FarmUpsert
import com.precisionfarming.mobile.data.PendingFarmStore
import com.precisionfarming.mobile.data.createFarm
import com.precisionfarming.mobile.data.pendingFarmKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateFarmPendingTest {
    private val body = FarmUpsert("Fazenda Rio Claro", "MT", 12.5, "America/Cuiaba")
    private val farm = FarmDto(id = "farm-1", name = body.name, location = body.location, areaHa = body.areaHa, timezone = body.timezone)

    private class FakeStore : PendingFarmStore {
        val map = mutableMapOf<String, FarmDto>()
        override fun get(key: String) = map[key]
        override fun put(key: String, farm: FarmDto) {
            map[key] = farm
        }
        override fun remove(key: String) {
            map.remove(key)
        }
        override fun clear() {
            map.clear()
        }
    }

    private inner class Harness(
        refreshOk: Boolean = true,
        initialRefresh: String? = "refresh-1",
    ) {
        val store = FakeStore()
        var posts = 0
        var refreshHttp = 0
        var refreshToken: String? = initialRefresh
        var refreshOk: Boolean = refreshOk

        suspend fun run(userId: String? = "user-1"): FarmDto =
            createFarm(
                userId = userId,
                body = body,
                store = store,
                postFarm = {
                    posts++
                    farm
                },
                consumeRefresh = { refreshToken.also { refreshToken = null } },
                refreshHttp = {
                    refreshHttp++
                    refreshOk
                },
            )
    }

    @Test
    fun postAndRefreshOkClearsPendingAndReturnsFarm() = runTest {
        val h = Harness()
        val result = h.run()
        assertEquals(farm, result)
        assertTrue(h.store.map.isEmpty())
        assertEquals(1, h.posts)
        assertEquals(1, h.refreshHttp)
    }

    @Test
    fun postAndRefreshFailKeepsPending() = runTest {
        val h = Harness(refreshOk = false)
        val thrown = runCatching { h.run() }.exceptionOrNull()
        assertTrue(thrown is IllegalStateException)
        assertEquals("SESSION_REFRESH_FAILED", thrown?.message)
        assertEquals(farm, h.store.get(pendingFarmKey("user-1", body)))
        assertEquals(1, h.posts)
        assertEquals(1, h.refreshHttp)
    }

    @Test
    fun retryAfterRefreshFailDoesNotPostAgain() = runTest {
        val h = Harness(refreshOk = false)
        runCatching { h.run() }
        runCatching { h.run() }
        assertEquals(1, h.posts)
        assertEquals(farm, h.store.get(pendingFarmKey("user-1", body)))
    }

    @Test
    fun afterConsumeSecondRetryDoesNotRefreshHttp() = runTest {
        val h = Harness(refreshOk = false)
        runCatching { h.run() }
        runCatching { h.run() }
        assertEquals(1, h.refreshHttp)
        assertNull(h.refreshToken)
    }

    @Test
    fun clearThenNextCallPostsAgain() = runTest {
        val h = Harness(refreshOk = false)
        runCatching { h.run() }
        h.store.clear()
        h.refreshToken = "refresh-2"
        runCatching { h.run() }
        assertEquals(2, h.posts)
    }

    @Test
    fun differentUserIdDoesNotHitAnotherUsersPending() = runTest {
        val h = Harness()
        h.store.put(pendingFarmKey("user-1", body), farm)
        val result = h.run(userId = "user-2")
        assertEquals(farm, result)
        assertEquals(1, h.posts)
        assertEquals(farm, h.store.get(pendingFarmKey("user-1", body)))
        assertNull(h.store.get(pendingFarmKey("user-2", body)))
    }
}
