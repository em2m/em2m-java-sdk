package io.em2m.simplex.parser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.node.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.*
import java.util.*

class ConditionsDeserializer(val simplex: Simplex) : JsonDeserializer<ConditionExpr>() {

    enum class BooleanOp { And, Or, Not }

    private fun parseValue(tree: TreeNode): String {
        return when (tree) {
            is TextNode -> tree.asText()
            is BooleanNode -> tree.asBoolean().toString()
            is ValueNode -> tree.toString()
            else -> throw IllegalArgumentException("Unexpected condition value type")
        }
    }

    private fun parseValueList(tree: TreeNode): List<String> {
        return when {
            tree.isValueNode -> listOf(parseValue(tree))
            tree.isArray -> (0 until tree.size()).map { parseValue(tree.get(it)) }
            else -> throw RuntimeException("Unexpected condition value type")
        }
    }

    private fun parseCondition(conditionType: String, tree: TreeNode): ConditionExpr {
        val conditions = tree.fieldNames().asSequence().toList().map { key ->
            val value = parseValueList(tree.get(key))
            simplex.compileCondition(conditionType, key, value)
        }
        return when {
            conditions.isEmpty() -> ConstConditionExpr(true)
            conditions.size == 1 -> conditions.first()
            else -> AndConditionExpr(conditions)
        }
    }

    private fun parseConditionObject(tree: ObjectNode): List<ConditionExpr> {
        return tree.fieldNames().asSequence().toList().map { fieldName ->
            val value = tree.get(fieldName)
            when (fieldName) {
                "And" -> parseCondition(value, BooleanOp.And)
                "Or" -> parseCondition(value, BooleanOp.Or)
                "Not" -> parseCondition(value, BooleanOp.Not)
                else -> parseCondition(fieldName, value)
            }
        }
    }

    fun parseCondition(tree: TreeNode, booleanOp: BooleanOp = BooleanOp.And): ConditionExpr {
        val conditions = ArrayList<ConditionExpr>()
        if (tree is ArrayNode) {
            tree.forEach {
                conditions.addAll(parseConditionObject(it as ObjectNode))
            }
        } else if (tree is ObjectNode) {
            conditions.addAll(parseConditionObject(tree))
        }
        return when {
            conditions.size == 0 -> {
                if (booleanOp == BooleanOp.Not) {
                    ConstConditionExpr(false)
                } else {
                    ConstConditionExpr(true)
                }
            }
            conditions.size == 1 -> {
                if (booleanOp == BooleanOp.Not) {
                    NotConditionExpr(conditions)
                } else conditions.first()
            }
            else -> {
                when (booleanOp) {
                    BooleanOp.Not -> NotConditionExpr(conditions)
                    BooleanOp.Or -> OrConditionExpr(conditions)
                    else -> AndConditionExpr(conditions)
                }
            }
        }
    }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): ConditionExpr {
        return try {
            val tree = parser.readValueAsTree<TreeNode>()
            parseCondition(tree, BooleanOp.And)
        } catch (e: RuntimeException) {
            throw JsonParseException(parser, e.message)
        }
    }

}