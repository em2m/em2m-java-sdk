package io.em2m.messages.redis

import io.em2m.flows.Processor
import io.em2m.messages.model.MessageContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

class RedisRuntime @Inject constructor(private val jedisPool: JedisPool, private val processor: Processor<MessageContext>) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private var shutdown: Boolean = false

    fun process(channel: String, message: String) {
        try {
            val ctx = MessageContext(environment = createEnvironment(channel))
            ctx.inputStream = message.byteInputStream()
            processor.process(ctx).subscribe()
        } catch (err: Throwable) {
            log.error("Error processing message", err)
        }
    }

    fun publishMessage(channel: String, message: String) {
        try {
            jedisPool.resource.use { cxn ->
                cxn.publish(channel, message)
            }
        } catch (e: Exception) {
            log.error("Error posting to redis", e)
        }
    }

    fun shutdown() {
        this.shutdown = true
        executor.shutdown()
    }

    fun shutdownNow() {
        this.shutdown = true
        executor.shutdownNow()
    }

    fun subscribe(vararg channels: String): RedisRuntime {
        val sub = Subscriber()
        executor.submit {
            do {
                try {
                    jedisPool.resource.subscribe(sub, *channels)
                } catch (e: Exception) {
                    log.error("Error connections to messages channel. Attempting reconnect")
                    Thread.sleep(5000)
                }
            } while (!shutdown)
        }
        return this
    }

    private fun createEnvironment(channel: String): MutableMap<String, Any?> {
        val currentTime = Date()
        return mapOf(
                "Channel" to channel,
                "CurrentTime" to currentTime
        ).toMutableMap()
    }

    private inner class Subscriber : JedisPubSub() {

        override fun onMessage(channel: String, message: String) {
            process(channel, message)
        }

        override fun onPMessage(pattern: String, channel: String, message: String) {
            onMessage(channel, message)
        }
    }

}