package io.em2m.actions.xforms

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.flows.Priorities
import io.em2m.flows.TransformerSupport
import org.xerial.snappy.SnappyInputStream
import rx.Observable
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

class JacksonRequestTransformer(val type: Class<out Any>, val objectMapper: ObjectMapper = jacksonObjectMapper())
    : TransformerSupport<ActionContext>(Priorities.PARSE) {

    override fun call(source: Observable<ActionContext>): Observable<ActionContext> {

        return source.doOnNext { context ->

            val contentType = context.environment["ContentType"] as? String
            val contentEncoding = context.environment["ContentEncoding"] as? String

            val inputStream = when (contentEncoding) {
                "gzip" -> GZIPInputStream(context.inputStream)
                "deflate" -> DeflaterInputStream(context.inputStream)
                "snappy" -> SnappyInputStream(context.inputStream)
                else -> context.inputStream
            }

            if (contentType?.contains("json") == true) {
                val obj = objectMapper.readValue(inputStream, type)
                context.request = obj
            } else if (contentType?.contains("text") == true) {
                val obj = objectMapper.readValue(inputStream, type)
                context.request = obj
            } else if (contentType?.contains("multipart") == true) {
                val form = context.multipart?.form
                if (form != null) {
                    context.request = objectMapper.convertValue(form, type)
                }
            }
        }
    }

}