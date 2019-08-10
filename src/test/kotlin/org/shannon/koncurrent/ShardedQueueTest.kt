package org.shannon.koncurrent

import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class ShardedQueueTest {
    class HashedNonce(private val value: Int) {
        override fun hashCode() = value
    }

    private val bufferSize = 10
    private val shardCount = 16
    private val queue = ShardedQueue<HashedNonce>(shardCount, bufferSize)

    @Test
    fun `given things when take then they are returned`() {
        runBlocking {
            var i = 0
            val given = List(10) { HashedNonce(i++) }
            given.forEach { queue.put(it) }
            val response = List(10) { queue.take(it) }
            assertThat(response).isEqualTo(given)
        }
    }

    @Test
    fun `given things with same hash when take then it is returned in order`() {
        runBlocking {
            val given = List(10) { HashedNonce(1) }
            given.forEach { queue.put(it) }
            val response = List(10) { queue.take(1) }
            assertThat(response).isEqualTo(given)
        }
    }

    @Test
    fun `given item for a shard when take then they are available only in that shard`() {
        runBlocking {
            val shard = 5
            val millis = async {
                measureTimeMillis {
                    queue.take(shard)
                }
            }
            launch { queue.put(HashedNonce(shard - 1)) }
            delay(1000)
            launch { queue.put(HashedNonce(shard)) }
            assertThat(millis.await()).isCloseTo(1000L, Offset.offset(100L))
        }
    }

    @Test
    fun `given items for all shards when take they are available for those shards`() {
        runBlocking {
            val millis = async {
                measureTimeMillis {
                    for (i in (0 until shardCount)) {
                        repeat(bufferSize) { queue.take(i) }
                    }
                }
            }
            delay(1000)
            repeat(bufferSize) {
                for (i in (0 until shardCount)) {
                    queue.put(HashedNonce(i))
                }
            }
            assertThat(millis.await()).isCloseTo(1000L, Offset.offset(100L))
        }
    }
/*
    @Test
    fun `given things with the same hash when take then they are synchronous`() {
        runBlocking {
            val given = List(bufferSize) { HashedNonce(1) }
            given.forEach { queue.put(it) }
            assertThat(measureTimeMillis {
                runBlocking {
                    repeat(given.size) {
                        launch {
                            val item = queue.take()
                            delay(1000)
                        }
                    }
                }
            }).isCloseTo(10L * 1000L, Offset.offset(500L))
            //queue.put(HashedNonce(1))
*/
/*            //assertThat(measureTimeMillis {
                given.forEach {
                    queue.put(it)
//                    delay(1000)
                }
            //}).isCloseTo(10L * 1000L, Offset.offset(500L))
            //assertThat(measureTimeMillis {
                repeat(given.size) {
                    launch {
                        queue.take()
//                        delay(1000)
                    }
                }
            //}).isCloseTo(10L * 1000L, Offset.offset(500L))*//*

        }
    }
*/
}