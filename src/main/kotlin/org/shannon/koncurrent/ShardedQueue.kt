package org.shannon.koncurrent

import kotlinx.coroutines.channels.*

// A KeyFactory will return some "Object" (Any is Kotlin for Object)
typealias KeyFactory<T> = (T) -> Any

/**
 * A ShardedQueue is really many queues in one. Incoming items are spread amongst the queues in a deterministic way
 * (here done by the modulus operator on the hashCode() of the key returned by the KeyFactory), such that all items
 * with the same key will end up in the same queue.
 *
 * Some collisions are inevitable, but more shards will cause fewer collisions if the key has a good hash function.
 *
 * When a particular inner queue gets overwhelmed (items in it are equal to the buffersize) then incoming items will block.
 */
class ShardedQueue<T>(val shardCount: Int, bufferSize: Int, val keyFactory: KeyFactory<T>) {
    // Defaulting the KeyFactory to using the given item's hash
    constructor(shardCount: Int, bufferSize: Int) : this(shardCount, bufferSize, { it as Any })

    // Channels operate much like Java's BlockingArrayQueue
    private val channels = List(shardCount) { Channel<T>(bufferSize) }

    private fun findShardIndex(item: T) = keyFactory(item).hashCode() % shardCount

    private fun findShard(item: T) = channels[findShardIndex(item)]

    suspend fun put(item: T) = findShard(item).send(item)

    suspend fun take(shardIndex: Int) = channels[shardIndex].receive()
}