package io.em2m.messages.aws

import com.fasterxml.jackson.databind.node.ObjectNode
import io.em2m.messages.model.MessageContext
import io.em2m.messages.model.TypedMessageFlow
import io.em2m.messages.xforms.JacksonMessageTransformer
import io.em2m.flows.BasicProcessor
import io.em2m.flows.Processor
import org.slf4j.LoggerFactory


class KinesisDevProcessor {

    companion object {

        val log = LoggerFactory.getLogger(javaClass)


        fun run() {
            val processor: Processor<MessageContext> = BasicProcessor.Builder<MessageContext>()
                    .transformer(JacksonMessageTransformer())
                    .flow(Logger::class)
                    .build()

            val config = KinesisRuntime.Config("pre-process", applicationName = "test-dev-processor")
            KinesisRuntime(config, processor, "Logger").run()
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