package io.em2m.utils.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode

fun JsonNode.getOrNull(fieldName: String): JsonNode? {
    val ret = this.get(fieldName)
    if (ret is NullNode) return null
    return ret
}

fun JsonNode.getStringOrNull(fieldName: String): String? {
    val ret = this.get(fieldName)
    if (ret is TextNode) return ret.asText(null)
    return null
}

fun JsonNode.getBooleanOrNull(fieldName: String): Boolean? {
    val ret = this.get(fieldName)
    if (ret is BooleanNode) return try { ret.asBoolean() } catch (_ : Exception) { null }
    return null
}

fun JsonNode.getBoolean(fieldName: String, default: Boolean = false): Boolean {
    return getBooleanOrNull(fieldName) ?: default
}

fun JsonNode.getIntegerOrNull(fieldName: String): Int? {
    val ret = this.get(fieldName)
    if (ret is IntNode) return try { ret.asInt() } catch (_ : Exception) { null }
    return null
}

fun JsonNode.getIntegerOrDefault(fieldName: String, default: Int): Int {
    return getIntegerOrNull(fieldName) ?: default
}
