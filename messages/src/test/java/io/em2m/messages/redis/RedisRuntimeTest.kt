package io.em2m.messages.redis

import io.em2m.flows.BasicProcessor
import io.em2m.flows.Processor
import io.em2m.messages.model.MessageContext
import io.em2m.messages.model.TypedMessageFlow
import io.em2m.messages.xforms.JacksonMessageTransformer
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool

@Ignore
class RedisRuntimeTest {

    lateinit var runtime: RedisRuntime

    companion object {
        val log = LoggerFactory.getLogger(javaClass)
    }

    @Before
    fun before() {

        val processor: Processor<MessageContext> = BasicProcessor.Builder<MessageContext>()
                .transformer(JacksonMessageTransformer())
                .flow(Logger::class)
                .build()

        val jedisPool = JedisPool()
        runtime = RedisRuntime(jedisPool, processor)
    }

    @Test
    fun testLog() {
        runtime.subscribe("Logger", "channel")
        runtime.publishMessage("channel", """{ "body": "test message"}""")
        Thread.sleep(2000)
        runtime.shutdownNow()
    }

    data class SimpleTypedMessage(val body: String)

    class Logger : TypedMessageFlow<SimpleTypedMessage>(SimpleTypedMessage::class.java) {
        override fun main(context: MessageContext, msg: SimpleTypedMessage) {
            log.debug(msg.body)
        }
    }

}