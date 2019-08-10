package org.shannon.koncurrent

import kotlinx.coroutines.channels.*

typealias KeyFactory<T> = (T) -> Any

class ShardedQueue<T>(val shardCount: Int, bufferSize: Int, val keyFactory: KeyFactory<T>) {
    constructor(shardCount: Int, bufferSize: Int) : this(shardCount, bufferSize, { it as Any })

    private val channels = List(shardCount) { Channel<T>(bufferSize) }

    private fun findShardIndex(item: T) = keyFactory(item).hashCode() % shardCount

    private fun findShard(item: T) = channels[findShardIndex(item)]

    suspend fun put(item: T) = findShard(item).send(item)

    suspend fun take(shardIndex: Int) = channels[shardIndex].receive()
}