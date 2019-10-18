# KoncurrentUtil

This project is for various concurrent utility capabilities "missing" in the Kotlin landscape.

# ShardedExecutor

In a microservices with distributed transaction environment, often you need to process transactions or messages for a key in the order in which they arrive or were applied to the source of truth.

For instance, consider an eventual consistency scenario where we make emails searchable. To begin with they get placed in S3 maybe with some metadata in a database. Then a service gets bucket notifications of every change to the email (created, updated, updated, deleted). The service's job is to put the contents into elasticsearch.

You'll want to process transactions on the email with some respect to its latest status. For instance, processing the delete before the create or processing updates out of order will result in data infidelity.

`ShardedExecutor` allows you to place these messages/transactions/events into one `executor` pick your key (could be user or the email id in the example above) and obtain a strong happens before guarantee. 

```
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
 ```

# ShardedQueue

A simple way of achieving the separation of concerns needed for the above is to take the hash of given object's key then place it in a different queue. This object performs that and presents a simplistic queue interface.

```
/**
 * A ShardedQueue is really many queues in one. Incoming items are spread amongst the queues in a deterministic way
 * (here done by the modulus operator on the hashCode() of the key returned by the KeyFactory), such that all items
 * with the same key will end up in the same queue.
 *
 * Some collisions are inevitable, but more shards will cause fewer collisions if the key has a good hash function.
 *
 * When a particular inner queue gets overwhelmed (items in it are equal to the buffersize) then incoming items will block.
 */
```
