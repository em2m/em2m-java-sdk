package io.em2m.simplex.model

import io.em2m.utils.coerce
import io.em2m.utils.convertValue
import kotlin.coroutines.coroutineContext

interface TreeExpr : Expr

class ArrayExpr(val values: List<Expr>) : TreeExpr {

    override fun call(context: ExprContext): List<Any?> {
        return values.filterNot {
            if (it is ObjectExpr) {
                val varContext = it.processVar(context)
                it.skip(varContext)
            } else false
        }.map { it.call(context) }.toList()
    }
}

class FieldExpr(val field: String, val value: Expr) : TreeExpr {

    override fun call(context: ExprContext): Pair<String, Any?> {
        return field to value.call(context)
    }
}

private class ObjectExprContext(val delegate: ExprContext) : ExprContext {
    override val entries: Set<Map.Entry<String, Any?>>
        get() = delegate.entries
    override val keys: Set<String>
        get() = delegate.keys
    override val size: Int
        get() = delegate.size
    override val values: Collection<Any?>
        get() = delegate.values

    override fun containsKey(key: String): Boolean {
        return delegate.containsKey(key)
    }

    override fun containsValue(value: Any?): Boolean {
        return delegate.containsValue(value)
    }

    override fun get(key: String): Any? {
        return delegate[key]
    }

    override fun isEmpty(): Boolean {
        return delegate.isEmpty()
    }

    var vars: Map<String, Any?> = emptyMap()

}

class ObjectExpr(val fields: List<FieldExpr>) : TreeExpr {

    private val fieldMap = fields.associateBy { it.field }

    fun skip(context: ExprContext): Boolean {
        val ifExpr = fieldMap["@if"]?.value ?: return false
        return when (val value = ifExpr.call(context)) {
            is Boolean -> !value
            is String -> value.isNullOrBlank()
            is Number -> (value == 0) || (value == Double.NaN)
            else -> value == null
        }
    }

    override fun call(context: ExprContext): Any? {
        val varContext = processVar(context)
        val skip = skip(varContext)
        return when {
            skip -> null
            fieldMap.containsKey("@repeat") -> processRepeat(varContext)
            fieldMap.containsKey("@when") -> processWhenArray(varContext)
            fieldMap.containsKey("@value") -> processValue(varContext)
            else -> processFields(varContext)
        }
    }

    private fun processValue(context: ExprContext): Any? {
        return fieldMap["@value"]?.value?.call(context)
    }

    internal fun processVar(context: ExprContext): ExprContext {
        val varField = fieldMap["@var"]?.value
        return if (varField != null) {
            val oldVariables: Map<String, Any> = context["variables"].coerce() ?: emptyMap()
            val newVariables: Map<String, Any?> = varField.call(context).coerce() ?: emptyMap()
            val variables = oldVariables.plus(newVariables)
            return context.plus("variables" to variables)
        } else context
    }

    class RepeatState(val size: Int) {
        var item: Any? = null
        var index: Int = 0
        val even: Boolean
            get() = (index % 2 == 0)
        val odd: Boolean
            get() = (index % 2 == 1)
        val first: Boolean
            get() = (index == 0)
        val last: Boolean
            get() = (index == size - 1)
    }

    private fun processRepeat(context: ExprContext): List<Any?> {
        val items: List<Any?> = (fieldMap["@repeat"]?.value)?.call(context).coerce() ?: emptyList()
        val valueExpr = fieldMap["@value"]?.value
        return if (items.isNotEmpty()) {
            val state = RepeatState(items.size)
            val repeatContext = context.plus("repeat" to state)
            items.mapIndexed { index, item ->
                state.item = item
                state.index = index
                if (valueExpr != null) {
                    valueExpr.call(repeatContext)
                } else {
                    processFields(repeatContext)
                }
            }
        } else emptyList()
    }

    private fun processContainerArray(value: Any?): Map<String, Any?> {
        val values: List<Map<String, Any?>> = value.coerce() ?: emptyList()
        return values.flatMap { it.toList() }.toMap()
    }

    // TODO - Support alternate non-object values (literals, arrays) for @when statements
    private fun processWhenArray(context: ExprContext): Any? {
        val expr = fieldMap["@when"]?.value as? ArrayExpr ?: return null
        expr.values.forEach {
            val value = (it as? ObjectExpr)?.call(context)
            if (value != null) {
                return value
            }
        }
        return null
    }

    private fun processFields(context: ExprContext): Map<String, Any?> {

        return fields.filter {
            // don't render annotations
            !it.field.startsWith("@") || it.field.startsWith("@container") || it.field.startsWith("@when")
        }.flatMap { field ->
            val key = field.field
            val value = field.value.call(context)
            // skip objects that return null
            if (field.value is ObjectExpr && value == null) {
                listOf(null)
            } else if (field.value is ObjectExpr && field.field.startsWith("@container")) {
                val map: Map<String, Any?> = value.coerce() ?: emptyMap()
                map.toList()
            } else if (field.value is ArrayExpr && field.field.startsWith("@container")) {
                processContainerArray(value).toList()
            } else {
                listOf(key to value)
            }
        }.filterNotNull().associate { it }
    }

}
