package io.em2m.simplex.parser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.Condition
import io.em2m.simplex.model.ConditionExpr
import java.util.*


class ConditionsDeserializer(val simplex: Simplex) : JsonDeserializer<ConditionExpr>() {

    private fun parseValue(tree: TreeNode): String {

        return when (tree) {
            is TextNode -> tree.asText()
            is BooleanNode -> tree.asBoolean().toString()
            is ValueNode -> tree.toString()
            else -> throw RuntimeException("Unexpected condition value type")
        }
    }

    private fun parseValueList(tree: TreeNode): List<String> {
        return when {
            tree.isValueNode -> listOf(parseValue(tree))
            tree.isArray -> (0 until tree.size()).map { parseValue(tree.get(it)) }
            else -> throw RuntimeException("Unexpected condition value type")
        }
    }

    private fun parseCondition(conditionType: String, tree: TreeNode): Sequence<Condition> {
        return tree.fieldNames().asSequence().map { key ->
            val value = parseValueList(tree.get(key))
            Condition(conditionType, key, value)
        }
    }

    private fun parseConditionObject(tree: ObjectNode): List<Condition> {
        return tree.fieldNames().asSequence().flatMap { conditionType ->
            parseCondition(conditionType, tree.get(conditionType))
        }.toList()
    }

    fun parseCondition(tree: TreeNode): ConditionExpr {
        val conditions = ArrayList<Condition>()
        if (tree is ArrayNode) {
            tree.forEach {
                conditions.addAll(parseConditionObject(it as ObjectNode))
            }
        } else if (tree is ObjectNode) {
            conditions.addAll(parseConditionObject(tree))
        }
        return simplex.compileCondition(conditions)
    }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): ConditionExpr {
        return try {
            val tree = parser.readValueAsTree<TreeNode>()
            parseCondition(tree)
        } catch (e: RuntimeException) {
            throw JsonParseException(parser, e.message)
        }
    }


}