package org.shannon.koncurrent

import kotlinx.coroutines.*

/**
 * A ShardedExecutor uses a ShardedQueue supplied by a some factory to split work into multiple
 * synchronous streams, thus ensuring that work items with the same key are given a strong
 * happens-before guarantee. Note: best practice would be to not use ShardedExecutor's queue for anything else.
 *
 * To use this, pass some lambda that returns a ShardedQueue<T> and a worker that will receive
 * your T and do the work you want done.
 *
 * ex: The following will print numbers from 0 to 99 with the happens-before guarantee that numbers with the same ones place will be ascending.
 *
 * data class Item(val key: Int, val value: Int)
 * val executor = ShardedExecutor(
 *      () -> ShardedQueue(shardCount = 10, bufferSize = 10, keyFactory = { it.key }), // a lambda taking a single variable can use the 'it' shorthand for that variable
 *      { print(it.value) }
 * )
 * for(i in 0..100) { executor.submit(Item(i % 10, i)) }
 *
 */
class ShardedExecutor<T>(queueFactory: () -> ShardedQueue<T>, worker: (T) -> Unit) {    // Note: Unit is Kotlin for 'void'
    private val queue = queueFactory()

    init {
        // This will launch a "background" coroutine per shard (hint: 'GlobalScope')
        for(shardIndex in 0 until queue.shardCount) {
            GlobalScope.launch { while(true) { worker(queue.take(shardIndex)) } }
        }
    }

    suspend fun submit(item: T) { queue.put(item) }
}