package io.em2m.messages.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.flows.Processor
import io.em2m.messages.model.MessageContext
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

class RedisRuntime @Inject constructor(private val jedisPool: JedisPool, private val processor: Processor<MessageContext>, private val mapper: ObjectMapper = jacksonObjectMapper()) {

    val log = LoggerFactory.getLogger(javaClass)
    val executor: ExecutorService = Executors.newCachedThreadPool()
    var shutdown: Boolean = false

    fun process(key: String, channel: String, message: String) {
        try {
            val ctx = MessageContext(environment = createEnvironment(channel))
            ctx.inputStream = message.byteInputStream()
            processor.process(key, ctx).subscribe()
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

    fun subscribe(key: String, vararg channels: String): RedisRuntime {
        val sub = Subscriber(key)
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

    private inner class Subscriber(val key: String) : JedisPubSub() {

        override fun onMessage(channel: String, message: String) {
            process(key, channel, message)
        }

        override fun onPMessage(pattern: String, channel: String, message: String) {
            onMessage(channel, message)
        }
    }

}