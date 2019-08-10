package org.shannon.koncurrent

import kotlinx.coroutines.*

class ShardedExecutor<T>(queueFactory: () -> ShardedQueue<T>, worker: (T) -> Unit) {
    private val queue = queueFactory()

    init {
        for(shardIndex in 0 until queue.shardCount) {
            GlobalScope.launch(Dispatchers.Unconfined) { while(true) { worker(queue.take(shardIndex)) } }
        }
    }

    suspend fun submit(item: T) { queue.put(item) }

}