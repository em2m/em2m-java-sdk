package io.em2m.simplex.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.*


class TreeDeserializer(simplex: Simplex = Simplex()) : JsonDeserializer<Expr>() {

    val parser = simplex.parser

    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): Expr {
        val node = jsonParser.readValueAsTree<TreeNode>()
        return parse(node as JsonNode)
    }

    private fun parse(node: JsonNode): Expr {
        return when (node) {
            is TextNode -> parser.parse(node.textValue())
            is DoubleNode -> SinglePartExpr(ConstPart(node.doubleValue()))
            is FloatNode -> SinglePartExpr(ConstPart(node.floatValue()))
            is LongNode -> SinglePartExpr(ConstPart(node.longValue()))
            is IntNode -> SinglePartExpr(ConstPart(node.intValue()))
            is ShortNode -> SinglePartExpr(ConstPart(node.shortValue()))
            is ArrayNode -> parseArray(node)
            is ObjectNode -> parseObject(node)
            else -> SinglePartExpr(ConstPart(null))
        }
    }

    private fun parseArray(node: ArrayNode): ArrayExpr {
        return ArrayExpr(node.map { parse(it) })
    }

    private fun parseObject(node: ObjectNode): ObjectExpr {
        val fields = node.fieldNames().asSequence()
        return ObjectExpr(fields.map { it to parse(node[it]) }.toMap())
    }

}
