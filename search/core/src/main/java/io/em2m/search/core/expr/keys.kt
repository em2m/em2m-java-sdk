package io.em2m.search.core.expr

import io.em2m.search.core.model.BucketContext
import io.em2m.search.core.model.RowContext
import io.em2m.simplex.model.*

interface Fielded {
    fun fields(key: Key): List<String>
}

interface FieldedExpr {
    fun fields(): List<String>
}

class FieldKeyHandler : KeyHandler, Fielded {

    override fun fields(key: Key): List<String> {
        return key.name.split(",").map { it.trim() }
    }

    override fun call(key: Key, context: ExprContext): Any? {
        val rowContext = RowContext(context)
        val values = fields(key).map { field ->
            rowContext.fieldValues[field]
        }
        return if (values.size == 1) {
            values.first()
        } else values
    }

    companion object {

        fun fields(expr: Expr): List<String> {
            return when (expr) {
                is TreeExpr -> treeFields(expr)
                is ValueExpr -> valueFields(expr)
                is ConditionExpr -> conditionFields(expr)
                is FieldedExpr -> expr.fields()
                else -> emptyList()
            }
        }

        private fun treeFields(expr: TreeExpr): List<String> {
            return when (expr) {
                is ArrayExpr -> arrayFields(expr)
                is ObjectExpr -> objectFields(expr)
                is FieldExpr -> fieldFields(expr)
                is ValueExpr -> valueFields(expr)
                else -> emptyList()
            }
        }

        private fun arrayFields(expr: ArrayExpr): List<String> {
            return expr.values.flatMap { fields(it) }
        }

        private fun objectFields(expr: ObjectExpr): List<String> {
            return expr.fields.flatMap { fields(it) }
        }

        private fun fieldFields(expr: FieldExpr): List<String> {
            return fields(expr.value)
        }

        private fun valueFields(expr: ValueExpr): List<String> {
            return expr.parts.flatMap { part ->
                if (part is PipePart) {
                    val handler = part.handler
                    val fields = mutableListOf<String>()
                    if (handler is Fielded) {
                        fields.addAll(handler.fields(part.key))
                    } else if (handler == null && (part.key.namespace == "field" || part.key.namespace == "f")) {
                        part.key.name.split(",").map { it.trim() }.forEach { fields.add(it) }
                    }
                    part.transforms.map { transform ->
                        if (transform is Fielded) {
                            fields.addAll(transform.fields(part.key))
                        }
                    }
                    fields
                } else emptyList<String>()
            }
        }

        private fun conditionFields(expr: ConditionExpr): List<String> {
            return when (expr) {
                is AndConditionExpr -> expr.conditions.flatMap { fields(it) }
                is OrConditionExpr -> expr.conditions.flatMap { fields(it) }
                is NotConditionExpr -> expr.conditions.flatMap { fields(it) }
                is SingleConditionExpr -> {
                    fields(expr.first).plus(fields(expr.second))
                }
                else -> emptyList()
            }
        }
    }
}

class BucketKeyKeyHandler : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return BucketContext(context).bucket.key
    }

}

class BucketCountKeyHandler : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return BucketContext(context).bucket.count
    }

}
