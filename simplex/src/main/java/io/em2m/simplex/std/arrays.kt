package io.em2m.simplex.std

import io.em2m.simplex.model.*
import io.em2m.utils.coerce

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

class FilterNotNullPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.filterNotNull()
            is Array<*> -> value.filterNotNull()
            else -> value
        }
    }
}

class FilterNotBlankPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.filterNot { it?.toString().isNullOrBlank() }
            is Array<*> -> value.filterNot { it?.toString().isNullOrBlank() }
            else -> value
        }
    }
}

class FirstPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.firstOrNull()
            is Array<*> -> value.firstOrNull()
            else -> value
        }
    }
}

class LastPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.lastOrNull()
            is Array<*> -> value.lastOrNull()
            else -> value
        }
    }
}

val StandardArrayConditions = emptyMap<String, ConditionHandler>()

object Arrays {
    val conditions = BasicConditionResolver(StandardBoolConditions)
    val pipes = BasicPipeTransformResolver(
            mapOf(
                    "notNull" to NotNullPipe(),
                    "reversed" to ReversedPipe(),
                    "filterNotNull" to FilterNotNullPipe(),
                    "filterNotBlank" to FilterNotBlankPipe(),
                    "first" to FirstPipe(),
                    "last" to LastPipe()
            )
    )
}