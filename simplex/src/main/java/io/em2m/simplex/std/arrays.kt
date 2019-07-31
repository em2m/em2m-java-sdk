package io.em2m.simplex.std

import io.em2m.simplex.model.*
import io.em2m.utils.coerce
import java.io.File.separator

class NotNullPipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.filterNotNull()
            is Array<*> -> value.filterNotNull()
            else -> value
        }
    }
}

class ReversedPipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.reversed()
            is Array<*> -> value.reversed()
            else -> value
        }
    }
}

val StandardArrayConditions = emptyMap<String, ConditionHandler>()


object Arrays {
    val conditions = BasicConditionResolver(StandardBoolConditions)
    val pipes = BasicPipeTransformResolver(mapOf("notNull" to NotNullPipe(), "reversed" to ReversedPipe()))
}