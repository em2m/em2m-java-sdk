package io.em2m.obj.jackson

import com.fasterxml.jackson.databind.JsonNode
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

