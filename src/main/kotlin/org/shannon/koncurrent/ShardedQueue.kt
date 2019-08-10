package org.shannon.koncurrent

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias KeyFactory<T> = (T) -> Any

val defaultKeyFactory: KeyFactory<Any> = { it }

class ShardedQueue<T>(val shardCount: Int, bufferSize: Int, val keyFactory: KeyFactory<T>) {
    constructor(shardCount: Int, bufferSize: Int) : this(shardCount, bufferSize, { it as Any })

    private val channels = List(shardCount) { Channel<T>(bufferSize) }
    //private val channel = Channel<T>(bufferSize)
    private val channelOut = Channel<T>(1)
    private val mutex = Mutex()

    fun findShardIndex(item: T) = keyFactory(item).hashCode() % shardCount

    private fun findShard(item: T) = channels[findShardIndex(item)]

    suspend fun put(item: T) = findShard(item).send(item)
        /*mutex.withLock {
            if (channelOut.isEmpty) {
                channelOut.send(item)
            } else {
                channelIn.send(item)
            }
        }*/
    //}

    suspend fun take(shardIndex: Int) = channels[shardIndex].receive()

    // TODO: not quite right
/*    suspend fun ack(item: T) {
        mutex.withLock {
            if (!channelIn.isEmpty) {
                channelOut.send(channelIn.receive())
            }
        }
    }*/
}