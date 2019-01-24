package io.em2m.simplex.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.*


class TreeDeserializer(val simplex: Simplex = Simplex()) : JsonDeserializer<Expr>() {

    val parser = simplex.parser

    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): Expr {
        val node = jsonParser.readValueAsTree<TreeNode>()
        return parse(node as JsonNode)
    }

    private fun parse(node: JsonNode, field: String? = null): Expr {
        return when (node) {
            is BinaryNode -> SinglePartExpr(ConstPart(node.binaryValue()))
            is BooleanNode -> SinglePartExpr(ConstPart(node.booleanValue()))
            is MissingNode -> SinglePartExpr(ConstPart(null))
            is NullNode -> SinglePartExpr(ConstPart(null))
            is POJONode -> SinglePartExpr(ConstPart(node.pojo))
            is TextNode -> parser.parse(node.textValue())
            is NumericNode -> SinglePartExpr(ConstPart(node.numberValue()))
            is ArrayNode -> parseArray(node)
            is ObjectNode -> parseObject(node)
            else -> SinglePartExpr(ConstPart(null))
        }
    }

    private fun parseArray(node: ArrayNode): ArrayExpr {
        return ArrayExpr(node.map { parse(it) })
    }

    private fun parseObject(node: ObjectNode): ObjectExpr {
        val fields = node.fieldNames().asSequence().map { f ->
            val fieldNode = node[f]
            FieldExpr(f, parse(fieldNode))
        }.toList()
        return ObjectExpr(fields)
    }

}
