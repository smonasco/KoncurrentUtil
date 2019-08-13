package org.shannon.koncurrent

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class ShardedExecutorTest {
    class Item(val key: Int, val value: Int) {
        override fun hashCode() = key
    }

    private val bufferSize = 1024
    private val shardCount = 1024
    private val processedItems = ConcurrentHashMap<Int, MutableList<Item>>()
    private val executor = ShardedExecutor({
        ShardedQueue<Item>(shardCount, bufferSize) { it.key }
    }, {
        processedItems.getOrPut(it.key, { mutableListOf() }).add(it)
    })

    @Test
    fun `given item expect it to be processed`() {
        runBlocking {
            val given = Item(1, 1)
            executor.submit(given)
            delay(1000)
            assertThat(processedItems[1]?.size).isEqualTo(1)
            assertThat(processedItems[1]?.get(0)).isEqualTo(given)
        }
    }

    @Test
    fun `given a bunch of things expect them to be processed in order with respect to their keys`() {
        runBlocking {
            val items = List(1_000_000) { Random.nextInt(0, 262144) }.mapIndexed { index, random: Int ->
                Item(key = random, value = index)
            }
            print(
                    measureTimeMillis {
                        runBlocking { items.forEach { launch { executor.submit(it) } } }
                    }
            )
            delay(10)
            assertThat(processedItems).isEqualTo(items.groupBy { it.key })
        }
    }
}