package io.em2m.messages.redis

import com.google.inject.Inject
import io.em2m.flows.Processor
import io.em2m.messages.model.MessageContext
import io.em2m.messages.model.MessageProcessorBuilder
import io.em2m.messages.model.TypedMessageFlow
import io.em2m.messages.xforms.JacksonMessageTransformer
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import redis.clients.jedis.JedisPool

@Ignore
class RedisRuntimeTest {

    lateinit var runtime: RedisRuntime

    @Before
    fun before() {
        val processor: Processor<MessageContext> = MessageProcessorBuilder()
                .flow({ it.channel == "channel" }, Logger::class)
                .flow({ true }, Logger::class)
                .transformer(JacksonMessageTransformer())
                .build()

        val jedisPool = JedisPool()
        runtime = RedisRuntime(jedisPool, processor)
    }

    @Test
    fun testLog() {
        runtime.subscribe("channel")
        runtime.publishMessage("channel", """{ "body": "test message"}""")
        Thread.sleep(2000)
        runtime.shutdownNow()
    }

    data class SimpleTypedMessage(val body: String)

    class Logger @Inject constructor() : TypedMessageFlow<SimpleTypedMessage>(SimpleTypedMessage::class.java) {
        override fun main(context: MessageContext, msg: SimpleTypedMessage) {
            println("Debug: ${msg.body}")
        }
    }

}