package com.precisionfarming.mobile.data.offline

import android.content.Context
import com.precisionfarming.mobile.data.EntityNames
import com.precisionfarming.mobile.data.PendingFarmCreates
import com.precisionfarming.mobile.data.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * App-scoped wiring: queue + connectivity + the offline→online flush trigger.
 * Initialised once from MainActivity like TokenStore; composables only read [queue]/[connectivity].
 */
object OfflineRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var initialized = false

    lateinit var queue: OfflineQueue
        private set
    lateinit var connectivity: ConnectivityMonitor
        private set

    val isReady: Boolean get() = initialized

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            PrefsQueueStore.wipeLegacy(app)
            val store = PrefsQueueStore.encrypted(app)?.let { PrefsQueueStore(it) } ?: InMemoryQueueStore()
            queue = OfflineQueue(store = store, executor = ApiCommandExecutor())
            connectivity = AndroidConnectivityMonitor(app)
            initialized = true
        }
        scope.launch {
            connectivity.online.collect { online ->
                if (online) flushIfSignedIn()
            }
        }
    }

    /** Drop anyone else's leftover commands after a successful login, then flush if already online. */
    fun bindSession(userId: String) {
        if (!initialized) return
        queue.retainForUser(userId)
        if (connectivity.online.value) {
            scope.launch { runCatching { queue.flush(userId = Session.userId ?: userId) } }
        }
    }

    /** Intentional logout: wipe the queue so the next operator cannot replay it. */
    fun onLogout() {
        EntityNames.clear()
        PendingFarmCreates.clear()
        if (initialized) queue.clear()
    }

    /** Enqueue and, when online, try to replay right away. Always returns the queued item. */
    fun enqueueAndSync(command: OpCommand): QueuedCommand {
        val queued = queue.enqueue(command, Session.userId)
        if (connectivity.online.value) scope.launch { flushIfSignedIn() }
        return queued
    }

    /** Manual "Sync now": forced flush ignoring backoff. Never replays another operator's commands. */
    suspend fun flushNow(): FlushResult {
        val userId = Session.userId ?: return FlushResult(0, 0, 0)
        return queue.flush(force = true, userId = userId)
    }

    private suspend fun flushIfSignedIn() {
        val userId = Session.userId ?: return
        runCatching { queue.flush(userId = userId) }
    }
}
