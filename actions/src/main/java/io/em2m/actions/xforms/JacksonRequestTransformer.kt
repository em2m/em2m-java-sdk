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
import io.em2m.simplex.evalPath
import io.em2m.utils.coerce
import io.em2m.utils.parseCharset
import org.xerial.snappy.SnappyInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

class JacksonRequestTransformer(
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
    override val priority: Int = Priorities.PARSE
) : ActionTransformer {

    override fun doOnNext(ctx: ActionContext) {

        val flow = ctx.flow
        val type = if (flow is TypedActionFlow<*, *>) {
            flow.requestType
        } else ObjectNode::class.java

        val contentType = (ctx.environment["ContentType"] as? String ?: "").lowercase(Locale.getDefault())

        val charset = parseCharset(contentType)

        if (contentType.contains("xml")) return

        try {
            if (contentType.contains("json") || contentType.contains("text")) {
                val inputStream = when (ctx.environment["ContentEncoding"] as? String) {
                    "gzip" -> GZIPInputStream(ctx.inputStream)
                    "deflate" -> DeflaterInputStream(ctx.inputStream)
                    "snappy" -> SnappyInputStream(ctx.inputStream)
                    else -> ctx.inputStream
                }
                val sanitizedInputStream = sanitizeInputStream(inputStream, charset)
                val obj = objectMapper.readValue(sanitizedInputStream, type)
//                val obj = objectMapper.readValue(inputStream, type)
                ctx.request = obj
            } else if (contentType.contains("multipart")) {
                val form = ctx.multipart?.form
                if (form != null) {
                    val body = form["body"]
                    val accept = form["accept"]
                    val filename = form["filename"]
                    val formContentType = form["contentType"]
                    if (formContentType != null) {
                        ctx.environment["ContentType"] = formContentType
                        if (formContentType.contains("json") && body != null) {
                            ctx.request = objectMapper.readValue(body, type)
                        } else {
                            ctx.request = objectMapper.convertValue(form, type)
                        }
                    } else {
                        ctx.request = objectMapper.convertValue(form, type)
                    }
                    if (accept != null) {
                        (ctx.environment["Headers"] as (MutableMap<String, Any?>))["accept"] = listOf(accept)
                    }
                    if (filename != null) {
                        ctx.response.headers.set("Content-Disposition", "attachment;filename=$filename")
                    }
                }
            } else if (contentType.contains("application/x-www-form-urlencoded")) {
                val paramMap: Map<String, List<Any>>? = ctx.environment["Parameters"]?.coerce()
                val body = paramMap
                    ?.mapNotNull { it.key to it.value.firstOrNull() }
                    ?.toMap()
                ctx.request = objectMapper.convertValue(body, type)
            }
        } catch (jsonEx: JsonProcessingException) {
            Problem(
                status = Problem.Status.BAD_REQUEST, title = "Error parsing JSON request", detail = jsonEx.message,
                ext = mapOf("line" to jsonEx.location.lineNr, "column" to jsonEx.location.columnNr)
            ).throwException()
        } catch (ioEx: IOException) {
            Problem(
                status = Problem.Status.BAD_REQUEST,
                title = "Error parsing request",
                detail = ioEx.message
            ).throwException()
        }
    }

    private fun sanitizeInputStream(inputStream: InputStream?, charset: Charset = Charsets.UTF_8): InputStream? {
        if (inputStream == null) {
            return null
        }

        val request = inputStream.reader(charset).use { it.readText() }
        val sanitized = removeScriptTags(request)
        return sanitized.byteInputStream(charset)
    }

    private fun removeScriptTags(input: String): String {
        val stringHasBeenUpdated = input.contains("<script>") || input.contains("</script>")

        val modifiedInput: String = input
            .replace("<script>", "")
            .replace("</script>", "")

        if (stringHasBeenUpdated) {
            return removeScriptTags(modifiedInput)
        }

        return modifiedInput
    }
}
