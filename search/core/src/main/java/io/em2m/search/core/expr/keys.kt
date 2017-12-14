package io.em2m.search.core.expr

import io.em2m.search.core.model.BucketContext
import io.em2m.search.core.model.RowContext
import io.em2m.simplex.model.ExprContext
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandler
import io.em2m.simplex.model.KeyHandlerSupport

class ConstKeyHandler(val value: Any?) : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return value
    }

}

class FieldKeyHandler : KeyHandler {

    override fun fields(key: Key): List<String> {
        return listOf(key.name)
    }

    override fun call(key: Key, context: ExprContext): Any? {
        return RowContext(context).fieldValues[key.name]
    }

}

class BucketKeyKeyHandler : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return BucketContext(context).bucket.key
    }

}
