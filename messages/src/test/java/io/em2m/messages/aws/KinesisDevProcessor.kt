package io.em2m.messages.aws

import com.fasterxml.jackson.databind.node.ObjectNode
import io.em2m.flows.Processor
import io.em2m.messages.model.MessageContext
import io.em2m.messages.model.MessageProcessorBuilder
import io.em2m.messages.model.TypedMessageFlow
import io.em2m.messages.xforms.JacksonMessageTransformer
import org.junit.Ignore
import org.slf4j.LoggerFactory


@Ignore
class KinesisDevProcessor {

    companion object {

        val log = LoggerFactory.getLogger(javaClass)

        fun run() {
            val processor: Processor<MessageContext> = MessageProcessorBuilder()
                    .flow({ true }, Logger::class)
                    .transformer(JacksonMessageTransformer())
                    .build()

            val config = KinesisRuntime.Config("pre-process", applicationName = "test-dev-processor")
            KinesisRuntime(config, processor).run()
        }
    }

    class Logger : TypedMessageFlow<ObjectNode>(ObjectNode::class.java) {

        override fun main(context: MessageContext, msg: ObjectNode) {
            log.debug(msg.toString())
        }

    }

}

fun main(args: Array<String>) {
    KinesisDevProcessor.run()
}