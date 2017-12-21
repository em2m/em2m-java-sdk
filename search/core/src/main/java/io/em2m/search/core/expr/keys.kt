package io.em2m.search.core.expr

import io.em2m.search.core.model.BucketContext
import io.em2m.search.core.model.RowContext
import io.em2m.simplex.model.*

class ConstKeyHandler(val value: Any?) : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return value
    }

}

interface Fielded {
    fun fields(key: Key): List<String>
}

class FieldKeyHandler : KeyHandler, Fielded {

    override fun fields(key: Key): List<String> {
        return listOf(key.name)
    }

    override fun call(key: Key, context: ExprContext): Any? {
        return RowContext(context).fieldValues[key.name]
    }

    companion object {
        fun fields(expr: Expr): List<String> {
            return expr.parts.flatMap { part ->
                if (part is PipePart) {
                    val handler = part.handler
                    if (handler is FieldKeyHandler) {
                        handler.fields(part.key)
                    } else emptyList<String>()
                } else emptyList()
            }
        }
    }
}

class BucketKeyKeyHandler : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return BucketContext(context).bucket.key
    }

}
