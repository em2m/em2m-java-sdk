package io.em2m.rules.parser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.node.*
import io.em2m.rules.Assertion
import io.em2m.rules.Assertions
import io.em2m.simplex.Simplex

class AssertionsDeserializer(val simplex: Simplex) : JsonDeserializer<Assertions>() {

    private fun parseValue(node: TreeNode): Any? {
        return when (node) {
            is BinaryNode -> node.binaryValue()
            is BooleanNode -> node.booleanValue()
            is MissingNode -> null
            is NullNode -> null
            is POJONode -> node.pojo
            is TextNode -> node.textValue()
            is NumericNode -> node.numberValue()
        // TODO - Unwrap arrays
            else -> throw IllegalArgumentException("Unexpected condition value type")
        }
    }

    private fun parseValueList(tree: TreeNode): List<Any?> {
        return when {
            tree.isValueNode -> listOf(parseValue(tree))
            tree.isArray -> (0 until tree.size()).map { parseValue(tree.get(it)) }
            else -> throw RuntimeException("Unexpected condition value type")
        }
    }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): Assertions {

        return try {
            val tree = parser.readValueAsTree<TreeNode>()
            return if (tree is ObjectNode) {
                Assertions(tree.fields().asSequence().map {
                    Assertion(it.key, parseValueList(it.value))
                }.toList())
            } else throw JsonParseException(parser, "Error parsing rules assertions")
        } catch (e: RuntimeException) {
            throw JsonParseException(parser, e.message)
        }
    }


}

