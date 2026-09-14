package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.offline.CommandExecutor
import com.precisionfarming.mobile.data.offline.ExecResult
import com.precisionfarming.mobile.data.offline.conflictAlreadyApplied
import com.precisionfarming.mobile.data.offline.InMemoryQueueStore
import com.precisionfarming.mobile.data.offline.OfflineQueue
import com.precisionfarming.mobile.data.offline.OpCommand
import com.precisionfarming.mobile.data.offline.OpCommandType
import com.precisionfarming.mobile.data.offline.QueueState
import com.precisionfarming.mobile.data.offline.QueuedCommand
import com.precisionfarming.mobile.data.offline.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class OfflineQueueTest {
    private class FakeExecutor(private val results: ArrayDeque<ExecResult>) : CommandExecutor {
        val seen = mutableListOf<QueuedCommand>()
        override suspend fun execute(command: QueuedCommand): ExecResult {
            seen += command
            return results.removeFirstOrNull() ?: ExecResult.Ok
        }
    }

    private var nowMs = 1_000_000L
    private val clock: () -> Instant = { Instant.ofEpochMilli(nowMs) }
    private var ids = 0
    private val idGen: () -> String = { "cid-${++ids}" }

    private fun queue(store: InMemoryQueueStore, executor: CommandExecutor, history: Int = 20) =
        OfflineQueue(store, executor, clock, idGen, { Duration.ofSeconds(10) }, history)

    @Test
    fun enqueueAssignsUniqueClientOperationIdAndPersists() {
        val store = InMemoryQueueStore()
        val q = queue(store, FakeExecutor(ArrayDeque()))
        val a = q.enqueue(OpCommand.Start("op-1"))
        val b = q.enqueue(OpCommand.Pause("op-1", "chuva"))
        assertNotEquals(a.clientOperationId, b.clientOperationId)
        assertEquals(SyncState.PENDING, a.state)
        assertEquals(OpCommandType.PAUSE, b.type)
        assertEquals("chuva", b.reason)
        assertEquals(2, store.saved.items.size)
        assertEquals(2, q.state.value.pendingCount)
        assertNotNull(q.state.value.openFor("op-1"))
        assertNull(q.state.value.lastSyncAt)
    }

    @Test
    fun successMarksSyncedAndRecordsLastSync() = runTest {
        val store = InMemoryQueueStore()
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Ok)))
        val q = queue(store, exec)
        val cmd = q.enqueue(OpCommand.Start("op-1"))
        val result = q.flush()
        assertEquals(1, result.synced)
        val item = q.state.value.items.single()
        assertEquals(SyncState.SYNCED, item.state)
        assertEquals(clock().toString(), item.syncedAt)
        assertEquals(clock().toString(), q.state.value.lastSyncAt)
        assertEquals(cmd.clientOperationId, exec.seen.single().clientOperationId)
        assertEquals(0, q.state.value.pendingCount)
    }

    @Test
    fun rejectedIsFailedAndNeverRetried() = runTest {
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Rejected("409 Conflict"))))
        val q = queue(InMemoryQueueStore(), exec)
        q.enqueue(OpCommand.Complete("op-1"))
        q.flush()
        val item = q.state.value.items.single()
        assertEquals(SyncState.FAILED, item.state)
        assertEquals("409 Conflict", item.lastError)
        assertEquals(1, item.attempts)
        q.flush(force = true)
        assertEquals(1, exec.seen.size)
        assertEquals(1, q.state.value.failedCount)
        q.dismiss(item.clientOperationId)
        assertTrue(q.state.value.items.isEmpty())
    }

    @Test
    fun transientFailureKeepsPendingWithBackoffAndStopsReplay() = runTest {
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Retry("timeout"), ExecResult.Ok, ExecResult.Ok)))
        val q = queue(InMemoryQueueStore(), exec)
        val first = q.enqueue(OpCommand.Start("op-1"))
        q.enqueue(OpCommand.Complete("op-1"))
        val r1 = q.flush()
        assertEquals(1, r1.retried)
        assertEquals(1, exec.seen.size) // stopped after the transient failure
        val pending = q.state.value.items.first { it.clientOperationId == first.clientOperationId }
        assertEquals(SyncState.PENDING, pending.state)
        assertEquals(1, pending.attempts)
        assertEquals(clock().plusSeconds(10).toString(), pending.nextAttemptAt)

        // Before the backoff window: nothing happens.
        val r2 = q.flush()
        assertEquals(0, r2.attempted)

        // After the window: both replay in order with the same client ids.
        nowMs += 11_000
        val r3 = q.flush()
        assertEquals(2, r3.synced)
        assertEquals(listOf(first.clientOperationId, first.clientOperationId, q.state.value.items[1].clientOperationId), exec.seen.map { it.clientOperationId })
        assertTrue(q.state.value.items.all { it.state == SyncState.SYNCED })
    }

    @Test
    fun forceIgnoresBackoff() = runTest {
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Retry("io"), ExecResult.Ok)))
        val q = queue(InMemoryQueueStore(), exec)
        q.enqueue(OpCommand.Start("op-1"))
        q.flush()
        val r = q.flush(force = true)
        assertEquals(1, r.synced)
    }

    @Test
    fun historyIsTrimmedToLastN() = runTest {
        val q = queue(InMemoryQueueStore(), FakeExecutor(ArrayDeque()), history = 2)
        repeat(4) { i ->
            nowMs += 1_000
            q.enqueue(OpCommand.Start("op-$i"))
        }
        q.flush()
        val synced = q.state.value.items.filter { it.state == SyncState.SYNCED }
        assertEquals(2, synced.size)
        assertEquals(listOf("op-2", "op-3"), synced.map { it.operationId })
    }

    @Test
    fun restoresPersistedState() {
        val persisted = QueueState(
            items = listOf(
                QueuedCommand("c1", "op-9", OpCommandType.START, createdAt = clock().toString(), state = SyncState.PENDING),
            ),
            lastSyncAt = "2026-01-01T00:00:00Z",
        )
        val q = queue(InMemoryQueueStore(persisted), FakeExecutor(ArrayDeque()))
        assertEquals(1, q.state.value.pendingCount)
        assertEquals("2026-01-01T00:00:00Z", q.state.value.lastSyncAt)
    }

    @Test
    fun syncingItemsAreRecoveredToPendingOnLoad() = runTest {
        val stuck = QueueState(
            items = listOf(
                QueuedCommand("c1", "op-1", OpCommandType.START, createdAt = clock().toString(), state = SyncState.SYNCING),
            ),
        )
        val store = InMemoryQueueStore(stuck)
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Ok)))
        val q = queue(store, exec)
        assertEquals(SyncState.PENDING, q.state.value.items.single().state)
        assertEquals(1, q.flush().synced)
        assertEquals(SyncState.SYNCED, q.state.value.items.single().state)
    }

    @Test
    fun corruptBackoffTimestampDoesNotAbortFlush() = runTest {
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Ok)))
        val broken = QueuedCommand(
            clientOperationId = "c1",
            operationId = "op-1",
            type = OpCommandType.START,
            createdAt = clock().toString(),
            nextAttemptAt = "not-an-instant",
        )
        val q = queue(InMemoryQueueStore(QueueState(items = listOf(broken))), exec)
        assertEquals(1, q.flush().synced)
    }

    @Test
    fun clearWipesTheQueue() {
        val q = queue(InMemoryQueueStore(), FakeExecutor(ArrayDeque()))
        q.enqueue(OpCommand.Start("op-1"), userId = "user-a")
        q.clear()
        assertTrue(q.state.value.items.isEmpty())
        assertNull(q.state.value.lastSyncAt)
    }

    @Test
    fun retainForUserDropsOtherOperatorsAndFlushIgnoresThem() = runTest {
        val store = InMemoryQueueStore()
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Ok)))
        val q = queue(store, exec)
        q.enqueue(OpCommand.Start("op-a"), userId = "user-a")
        q.enqueue(OpCommand.Start("op-b"), userId = "user-b")
        q.retainForUser("user-b")
        assertEquals(listOf("op-b"), q.state.value.items.map { it.operationId })
        assertEquals(1, q.flush(userId = "user-b").synced)
        assertEquals(listOf("op-b"), exec.seen.map { it.operationId })
    }

    @Test
    fun flushWithUserIdDoesNotReplayAnotherUsersCommands() = runTest {
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Ok, ExecResult.Ok)))
        val q = queue(InMemoryQueueStore(), exec)
        q.enqueue(OpCommand.Start("op-a"), userId = "user-a")
        q.enqueue(OpCommand.Start("op-b"), userId = "user-b")
        val result = q.flush(userId = "user-b")
        assertEquals(1, result.synced)
        assertEquals(listOf("op-b"), exec.seen.map { it.operationId })
        assertEquals(SyncState.PENDING, q.state.value.items.first { it.operationId == "op-a" }.state)
    }

    @Test
    fun defaultBackoffGrowsAndCaps() {
        assertEquals(5L, OfflineQueue.defaultBackoff(1).seconds)
        assertEquals(10L, OfflineQueue.defaultBackoff(2).seconds)
        assertEquals(40L, OfflineQueue.defaultBackoff(4).seconds)
        assertEquals(300L, OfflineQueue.defaultBackoff(12).seconds)
    }

    @Test
    fun http2xxMarksSynced() = runTest {
        val q = queue(InMemoryQueueStore(), FakeExecutor(ArrayDeque(listOf(ExecResult.Ok))))
        q.enqueue(OpCommand.Start("op-1"))
        val result = q.flush()
        assertEquals(1, result.synced)
        assertEquals(0, result.failed)
        assertEquals(0, result.retried)
        assertEquals(SyncState.SYNCED, q.state.value.items.single().state)
    }

    @Test
    fun http4xxFailsAndIsNeverRetriedEvenWithForce() = runTest {
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Rejected("400 Bad Request"), ExecResult.Ok)))
        val q = queue(InMemoryQueueStore(), exec)
        q.enqueue(OpCommand.Pause("op-1", "chuva"))
        assertEquals(1, q.flush().failed)
        assertEquals(SyncState.FAILED, q.state.value.items.single().state)
        assertEquals(0, q.flush(force = true).attempted)
        assertEquals(1, exec.seen.size)
        assertEquals(SyncState.FAILED, q.state.value.items.single().state)
    }

    @Test
    fun ioRetryAppliesBackoffAndDoesNotSkipLaterCommands() = runTest {
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Retry("io"), ExecResult.Ok, ExecResult.Ok, ExecResult.Ok)))
        val q = queue(InMemoryQueueStore(), exec)
        q.enqueue(OpCommand.Start("op-1"))
        q.enqueue(OpCommand.Complete("op-1"))
        q.enqueue(OpCommand.Start("op-2"))
        val first = q.flush()
        assertEquals(1, first.retried)
        assertEquals(listOf(OpCommandType.START), exec.seen.map { it.type })
        assertEquals(listOf("op-1"), exec.seen.map { it.operationId })
        assertTrue(q.state.value.items.drop(1).all { it.state == SyncState.PENDING })

        nowMs += 5_000
        assertEquals(0, q.flush().attempted)

        nowMs += 6_000
        val after = q.flush()
        assertEquals(3, after.synced)
        assertEquals(
            listOf(OpCommandType.START, OpCommandType.START, OpCommandType.COMPLETE, OpCommandType.START),
            exec.seen.map { it.type },
        )
        assertEquals(listOf("op-1", "op-1", "op-1", "op-2"), exec.seen.map { it.operationId })
        assertTrue(q.state.value.items.all { it.state == SyncState.SYNCED })
    }

    @Test
    fun replayKeepsPerOpCreationOrderAndRejectedDoesNotBlockLaterOps() = runTest {
        val exec = FakeExecutor(
            ArrayDeque(listOf(ExecResult.Rejected("409"), ExecResult.Ok, ExecResult.Ok)),
        )
        val q = queue(InMemoryQueueStore(), exec)
        q.enqueue(OpCommand.Start("op-1"))
        q.enqueue(OpCommand.Complete("op-1"))
        q.enqueue(OpCommand.Start("op-2"))
        val result = q.flush()
        assertEquals(1, result.failed)
        assertEquals(2, result.synced)
        assertEquals(listOf("op-1", "op-1", "op-2"), exec.seen.map { it.operationId })
        assertEquals(listOf(OpCommandType.START, OpCommandType.COMPLETE, OpCommandType.START), exec.seen.map { it.type })
        val items = q.state.value.items
        assertEquals(SyncState.FAILED, items[0].state)
        assertEquals(SyncState.SYNCED, items[1].state)
        assertEquals(SyncState.SYNCED, items[2].state)
    }

    @Test
    fun inMemoryQueueStoreRoundTripsQueuedState() = runTest {
        val store = InMemoryQueueStore()
        val q1 = queue(store, FakeExecutor(ArrayDeque(listOf(ExecResult.Ok))))
        val queued = q1.enqueue(OpCommand.Pause("op-1", "chuva"))
        q1.flush()
        q1.recordSync()
        val snapshot = store.saved
        assertEquals(queued.clientOperationId, snapshot.items.single().clientOperationId)
        assertEquals(SyncState.SYNCED, snapshot.items.single().state)
        assertEquals("chuva", snapshot.items.single().reason)
        assertNotNull(snapshot.lastSyncAt)

        val restored = InMemoryQueueStore(snapshot)
        assertEquals(snapshot, restored.load())
        val q2 = queue(restored, FakeExecutor(ArrayDeque()))
        assertEquals(snapshot.items, q2.state.value.items)
        assertEquals(snapshot.lastSyncAt, q2.state.value.lastSyncAt)
        assertEquals("chuva", q2.state.value.items.single().reason)
        assertEquals(0, q2.state.value.pendingCount)
    }

    @Test
    fun flushOnReconnectSyncsPendingCommands() = runTest {
        val online = MutableStateFlow(false)
        val exec = FakeExecutor(ArrayDeque(listOf(ExecResult.Ok)))
        val q = queue(InMemoryQueueStore(), exec)
        q.enqueue(OpCommand.Start("op-1"))
        val job = launch {
            online.first { it }
            q.flush()
        }
        testScheduler.runCurrent()
        assertEquals(0, exec.seen.size)
        assertEquals(SyncState.PENDING, q.state.value.items.single().state)

        online.value = true
        job.join()
        assertEquals(1, exec.seen.size)
        assertEquals(1, q.state.value.items.count { it.state == SyncState.SYNCED })
        assertEquals(0, q.state.value.pendingCount)
    }

    @Test
    fun conflictAlreadyAppliedWhenServerAlreadyMoved() {
        assertTrue(conflictAlreadyApplied(OpCommandType.START, "IN_PROGRESS"))
        assertTrue(conflictAlreadyApplied(OpCommandType.START, "STARTING"))
        assertTrue(conflictAlreadyApplied(OpCommandType.PAUSE, "PAUSED"))
        assertTrue(conflictAlreadyApplied(OpCommandType.COMPLETE, "COMPLETING"))
        assertFalse(conflictAlreadyApplied(OpCommandType.START, "PAUSED"))
        assertFalse(conflictAlreadyApplied(OpCommandType.START, "PLANNED"))
        assertFalse(conflictAlreadyApplied(OpCommandType.PAUSE, "IN_PROGRESS"))
        assertFalse(conflictAlreadyApplied(OpCommandType.COMPLETE, "PAUSED"))
    }

    @Test
    fun projectedStatusUsesLatestSyncedAndKeepsLaterServerState() {
        val queue = QueueState(
            items = listOf(
                QueuedCommand(
                    "c1",
                    "op-1",
                    OpCommandType.START,
                    createdAt = "2026-01-01T00:00:00Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:01Z",
                ),
                QueuedCommand(
                    "c2",
                    "op-1",
                    OpCommandType.PAUSE,
                    createdAt = "2026-01-01T00:00:02Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:03Z",
                ),
            ),
        )
        assertEquals("PAUSED", queue.projectedStatus("op-1", "PLANNED"))
        assertEquals("COMPLETED", queue.projectedStatus("op-1", "COMPLETED"))
        assertEquals("PLANNED", queue.projectedStatus("op-other", "PLANNED"))
    }

    @Test
    fun projectedStatusPrefersServerPauseOverLocalStart() {
        val started = QueueState(
            items = listOf(
                QueuedCommand(
                    "c1",
                    "op-1",
                    OpCommandType.START,
                    createdAt = "2026-01-01T00:00:00Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:01Z",
                ),
            ),
        )
        assertEquals("PAUSED", started.projectedStatus("op-1", "PAUSED"))
        assertEquals("IN_PROGRESS", started.projectedStatus("op-1", "PLANNED"))

        val paused = QueueState(
            items = listOf(
                QueuedCommand(
                    "c2",
                    "op-1",
                    OpCommandType.PAUSE,
                    createdAt = "2026-01-01T00:00:02Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:03Z",
                ),
            ),
        )
        assertEquals("PAUSED", paused.projectedStatus("op-1", "IN_PROGRESS"))
    }

    @Test
    fun projectedStatusUsesLatestSyncedAndKeepsLaterServerState() {
        val queue = QueueState(
            items = listOf(
                QueuedCommand(
                    "c1",
                    "op-1",
                    OpCommandType.START,
                    createdAt = "2026-01-01T00:00:00Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:01Z",
                ),
                QueuedCommand(
                    "c2",
                    "op-1",
                    OpCommandType.PAUSE,
                    createdAt = "2026-01-01T00:00:02Z",
                    state = SyncState.SYNCED,
                    syncedAt = "2026-01-01T00:00:03Z",
                ),
            ),
        )
        assertEquals("PAUSED", queue.projectedStatus("op-1", "PLANNED"))
        assertEquals("COMPLETED", queue.projectedStatus("op-1", "COMPLETED"))
        assertEquals("PLANNED", queue.projectedStatus("op-other", "PLANNED"))
    }
}
