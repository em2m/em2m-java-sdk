package io.em2m.actions.xforms

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Priorities
import io.em2m.actions.model.TypedActionFlow
import io.em2m.problem.Problem
import org.xerial.snappy.SnappyInputStream
import java.io.IOException
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

class JacksonRequestTransformer(private val objectMapper: ObjectMapper = jacksonObjectMapper(), override val priority: Int = Priorities.PARSE) : ActionTransformer {

    override fun doOnNext(ctx: ActionContext) {

        val flow = ctx.flow
        val type = if (flow is TypedActionFlow<*, *>) {
            flow.requestType
        } else ObjectNode::class.java

        val contentType = ctx.environment["ContentType"] as? String

        val inputStream = when (ctx.environment["ContentEncoding"] as? String) {
            "gzip" -> GZIPInputStream(ctx.inputStream)
            "deflate" -> DeflaterInputStream(ctx.inputStream)
            "snappy" -> SnappyInputStream(ctx.inputStream)
            else -> ctx.inputStream
        }

        try {
            if (contentType?.contains("json") == true) {
                val obj = objectMapper.readValue(inputStream, type)
                ctx.request = obj
            } else if (contentType?.contains("text") == true) {
                val obj = objectMapper.readValue(inputStream, type)
                ctx.request = obj
            } else if (contentType?.contains("multipart") == true) {
                val form = ctx.multipart?.form
                if (form != null) {
                    ctx.request = objectMapper.convertValue(form, type)
                }
            }
        } catch (jsonEx: JsonProcessingException) {
            Problem(status = Problem.Status.BAD_REQUEST, title = "Error parsing JSON request", detail = jsonEx.message,
                    ext = mapOf("line" to jsonEx.location.lineNr, "column" to jsonEx.location.columnNr)).throwException()
        } catch (ioEx: IOException) {
            Problem(status = Problem.Status.BAD_REQUEST, title = "Error parsing request", detail = ioEx.message).throwException()
        }
    }
}