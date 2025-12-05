package io.em2m.actions.xforms

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Priorities
import io.em2m.actions.model.TypedActionFlow
import io.em2m.problem.Problem
import io.em2m.utils.coerce
import io.em2m.utils.parseCharset
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import org.xerial.snappy.SnappyInputStream
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

    private val sanitizer = HtmlSanitizer(objectMapper)

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
                status = Problem.Status.BAD_REQUEST,
                title = "Error parsing JSON request",
                detail = jsonEx.message,
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
        val sanitized = sanitizer.sanitizePayload(request)
        return sanitized.byteInputStream(charset)
    }

    internal class HtmlSanitizer(private val objectMapper: ObjectMapper) {

        private val outputSettings = Document.OutputSettings().prettyPrint(false)
        private val safelist = createSafelist()

        fun sanitizePayload(raw: String): String {
            if (raw.isBlank()) return raw

            return runCatching {
                val jsonNode = objectMapper.readTree(raw)
                val sanitized = sanitizeNode(jsonNode)
                objectMapper.writeValueAsString(sanitized)
            }.getOrElse {
                sanitizeText(raw)
            }
        }

        private fun sanitizeNode(node: JsonNode?): JsonNode? {
            return when {
                node == null -> null
                node.isObject -> sanitizeObject(node)
                node.isArray -> sanitizeArray(node)
                node.isTextual -> TextNode(sanitizeText(node.asText()))
                else -> node
            }
        }

        private fun sanitizeObject(node: JsonNode): JsonNode {
            val sanitized = objectMapper.createObjectNode()
            node.fields().forEach { (key, value) ->
                sanitized.set<JsonNode?>(key, sanitizeNode(value))
            }
            return sanitized
        }

        private fun sanitizeArray(node: JsonNode): JsonNode {
            val sanitized = objectMapper.createArrayNode()
            node.forEach { child ->
                sanitized.add(sanitizeNode(child))
            }
            return sanitized
        }

        private fun sanitizeText(input: String): String {
            if (input.isBlank()) return ""

            val withoutScripts = SCRIPT_TAG_REGEX.replace(input, "")
            if (withoutScripts.isBlank()) return ""

            val cleaned = Jsoup.clean(withoutScripts, "", safelist, outputSettings)
            return cleaned.trim()
        }

        private fun createSafelist(): Safelist {
            return Safelist.none()
                .addTags("a", "b", "br", "div", "font", "i", "img", "li", "p", "span", "style", "sup", "u", "ul")
                .addAttributes("a", "href", "title", "target")
                .addAttributes("img", "src", "alt", "title", "width", "height")
                .addAttributes("font", "size", "color", "face")
                .addAttributes("div", "style")
                .addAttributes("span", "style")
                .addAttributes("p", "style")
                .addAttributes("sup", "style")
                .addAttributes("ul", "style")
                .addAttributes("li", "style")
                .addAttributes("b", "style")
                .addAttributes("i", "style")
                .addAttributes("u", "style")
                .addAttributes("style", "type")
                .addProtocols("a", "href", "http", "https", "mailto", "tel")
                .addProtocols("img", "src", "http", "https")
                .addEnforcedAttribute("a", "rel", "noopener noreferrer")
        }

        companion object {
            private val SCRIPT_TAG_REGEX = Regex("(?is)</?script[^>]*>")
        }
    }
}
