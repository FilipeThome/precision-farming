package com.precisionfarming.common.concurrency

import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Virtual-thread fan-out for blocking I/O (HTTP, JDBC wait).
 * JDBC still needs a bounded Hikari pool; VTs only free platform threads.
 */
object VirtualJobs {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun <T> all(tasks: List<Callable<T>>): List<T> =
        when {
            tasks.isEmpty() -> emptyList()
            tasks.size == 1 -> listOf(tasks[0].call())
            else -> executor.invokeAll(tasks).map { it.get() }
        }

    fun <A, B> zip(left: Callable<A>, right: Callable<B>): Pair<A, B> {
        val a = executor.submit(left)
        val b = executor.submit(right)
        return a.get() to b.get()
    }

    fun runAll(tasks: List<Runnable>) {
        if (tasks.size <= 1) {
            tasks.forEach { it.run() }
            return
        }
        tasks.map { executor.submit(it) }.forEach { it.get() }
    }
}
