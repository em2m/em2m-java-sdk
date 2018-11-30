package io.em2m.messages.xforms

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.flows.Priorities
import io.em2m.flows.Transformer
import io.em2m.messages.model.MessageContext
import io.em2m.messages.model.TypedMessageFlow
import org.xerial.snappy.SnappyInputStream
import rx.Observable
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

class JacksonMessageTransformer(private val objectMapper: ObjectMapper = jacksonObjectMapper(), override val priority: Int = Priorities.PARSE)
    : Transformer<MessageContext> {

    override fun call(source: Observable<MessageContext>): Observable<MessageContext> {

        return source.doOnNext { context ->

            val flow = context.flow
            val type = if (flow is TypedMessageFlow<*>) {
                flow.messageType
            } else ObjectNode::class.java

            val contentType = context.environment["ContentType"] as? String
            val contentEncoding = context.environment["ContentEncoding"] as? String

            val inputStream = when (contentEncoding) {
                "gzip" -> GZIPInputStream(context.inputStream)
                "deflate" -> DeflaterInputStream(context.inputStream)
                "snappy" -> SnappyInputStream(context.inputStream)
                else -> context.inputStream
            }

            val obj = objectMapper.readValue(inputStream, type)
            context.message = obj
        }
    }

}