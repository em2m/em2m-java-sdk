package io.em2m.simplex.std

import io.em2m.simplex.model.*

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

class FirstNotBlankPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.find { !it?.toString().isNullOrBlank() }
            is Array<*> -> value.find { !it?.toString().isNullOrBlank() }
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

class LastNotBlankPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.findLast { !it?.toString().isNullOrBlank() }
            is Array<*> -> value.findLast { !it?.toString().isNullOrBlank() }
            else -> value
        }
    }
}

class SizePipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.size
            is Array<*> -> value.size
            is String -> value.length
            else -> null
        }
    }
}

class SlicePipe : PipeTransform {

    var range = IntRange(0, 0)

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            val start = if (args.isNotEmpty()) {
                try {args[0].toInt()} catch (ex: RuntimeException) { 0 }
            } else { 0 }
            val end = if (args.size > 1) {
                try {args[1].toInt()} catch (ex: RuntimeException) { 0 }
            } else { 0 }
            range = IntRange(start, end)
        }
    }
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.slice(range)
            is Array<*> -> value.slice(range)
            is String -> value.slice(range)
            else -> null
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
                    "firstNotBlank" to FirstNotBlankPipe(),
                    "last" to LastPipe(),
                    "lastNotBlank" to LastNotBlankPipe(),
                    "size" to SizePipe(),
                    "slice" to SlicePipe()
            )
    )
}