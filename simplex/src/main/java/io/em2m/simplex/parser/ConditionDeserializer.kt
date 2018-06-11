package io.em2m.simplex.parser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.node.*
import io.em2m.simplex.model.Condition
import java.util.*


class ConditionsDeserializer : JsonDeserializer<List<Condition>>() {

    fun parseValue(tree: TreeNode): String {

        return when (tree) {
            is TextNode -> tree.asText()
            is BooleanNode -> tree.asBoolean().toString()
            is ValueNode -> tree.toString()
            else -> throw RuntimeException("Unexpected condition value type")
        }
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

    fun parseCondition(conditionType: String, tree: TreeNode): Sequence<Condition> {
        return tree.fieldNames().asSequence().map { key ->
            val value = parseValueList(tree.get(key))
            Condition(conditionType, key, value)
        }
    }

    fun parseConditionObject(tree: ObjectNode): List<Condition> {
        return tree.fieldNames().asSequence().flatMap { conditionType ->
            parseCondition(conditionType, tree.get(conditionType))
        }.toList()
    }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): List<Condition> {

        return try {
            val conditions = ArrayList<Condition>()
            val tree = parser.readValueAsTree<TreeNode>()
            if (tree is ArrayNode) {
                tree.forEach {
                    conditions.addAll(parseConditionObject(it as ObjectNode))
                }
            } else if (tree is ObjectNode) {
                conditions.addAll(parseConditionObject(tree))
            }
            conditions
        } catch (e: RuntimeException) {
            throw JsonParseException(parser, e.message)
        }
    }

}