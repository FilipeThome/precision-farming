package com.precisionfarming.mobile.data.offline

/** Persistence seam for the queue. Implementations must be cheap and never throw. */
interface QueueStore {
    fun load(): QueueState
    fun save(state: QueueState)
}

class InMemoryQueueStore(initial: QueueState = QueueState()) : QueueStore {
    var saved: QueueState = initial
        private set
    var saveCount: Int = 0
        private set

    override fun load(): QueueState = saved

    override fun save(state: QueueState) {
        saved = state
        saveCount++
    }
}
