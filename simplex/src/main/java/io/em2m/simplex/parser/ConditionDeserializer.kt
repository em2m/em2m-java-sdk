package io.em2m.simplex.parser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.node.TextNode
import io.em2m.simplex.model.Condition
import java.util.*


class ConditionsDeserializer : JsonDeserializer<List<Condition>>() {

    fun parseValue(tree: TreeNode): String {
        return if (tree is TextNode) {
            tree.asText()
        } else throw RuntimeException("Unexpected condition value type")
    }

    fun parseValueList(tree: TreeNode): List<String> {
        return if (tree.isValueNode) {
            listOf(parseValue(tree))
        } else if (tree.isArray) {
            (0 until tree.size()).map { parseValue(tree.get(it)) }
        } else {
            throw RuntimeException("Unexpected condition value type")
        }
    }

    fun parseCondition(conditionType: String, tree: TreeNode): Condition {
        tree.fieldNames().forEach { key ->
            val value = parseValueList(tree.get(key))
            return Condition(conditionType, key, value)
        }
        throw RuntimeException("Error parsing Condition")
    }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): List<Condition> {

        val conditions = ArrayList<Condition>()

        val tree = parser.readValueAsTree<TreeNode>()
        require(tree.isObject)

        tree.fieldNames().forEach { conditionType ->
            try {
                conditions.add(parseCondition(conditionType, tree.get(conditionType)))
            } catch (e: RuntimeException) {
                throw JsonParseException(parser, e.message)
            }
        }

        return conditions
    }

}
