package io.em2m.simplex.std

import com.fasterxml.jackson.databind.node.ArrayNode
import io.em2m.simplex.evalPath
import io.em2m.simplex.model.*
import io.em2m.utils.coerce

class NotNullPipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.filterNotNull()
            is Array<*> -> value.filterNotNull()
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class ReversedPipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.reversed()
            is Array<*> -> value.reversed()
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class FilterNotNullPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.filterNotNull()
            is Array<*> -> value.filterNotNull()
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class FilterNotBlankPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.filterNot { it?.toString().isNullOrBlank() }
            is Array<*> -> value.filterNot { it?.toString().isNullOrBlank() }
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class FirstPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.firstOrNull()
            is Array<*> -> value.firstOrNull()
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class FirstNotBlankPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.find { !it?.toString().isNullOrBlank() }
            is Array<*> -> value.find { !it?.toString().isNullOrBlank() }
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class LastPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.lastOrNull()
            is Array<*> -> value.lastOrNull()
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class LastNotBlankPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.findLast { !it?.toString().isNullOrBlank() }
            is Array<*> -> value.findLast { !it?.toString().isNullOrBlank() }
            is ArrayNode -> value.firstOrNull()
            else -> value
        }
    }
}

class SizePipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.size
            is Array<*> -> value.size
            is ArrayNode -> value.size()
            is String -> value.length
            else -> null
        }
    }
}

class TakePipe : PipeTransform {
    var n = 0

    override fun args(args: List<String>) {
        n = args.firstOrNull()?.toInt() ?:0
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.take(n)
            is Array<*> -> value.take(n)
            is ArrayNode -> value.take(n)
            is String -> value.take(n)
            else -> null
        }
    }
}

class TakeLastPipe : PipeTransform {
    var n = 0

    override fun args(args: List<String>) {
        n = args.firstOrNull()?.toInt() ?:0
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.takeLast(n)
            is Array<*> -> value.takeLast(n)
            is ArrayNode -> convertToList(value)?.takeLast(n)
            is String -> value.takeLast(n)
            else -> null
        }
    }

    private fun convertToList(node: ArrayNode): List<*>? {
        return node.coerce()
    }
}

class SlicePipe : PipeTransform {

    var range = IntRange(0, 0)

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            val start = if (args.isNotEmpty()) {
                try {
                    args[0].toInt()
                } catch (ex: RuntimeException) {
                    0
                }
            } else {
                0
            }
            val end = if (args.size > 1) {
                try {
                    args[1].toInt()
                } catch (ex: RuntimeException) {
                    0
                }
            } else {
                0
            }
            range = IntRange(start, end)
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.slice(range)
            is Array<*> -> value.slice(range)
            is ArrayNode -> value.firstOrNull()
            is String -> value.slice(range)
            else -> null
        }
    }
}

class AssociateByPipe() : PipeTransform {

    private var path: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            path = args[0].trim()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when {
            path.isNullOrEmpty() -> null
            (value is List<*>) -> transformList(value)
            (value is Array<*>) -> transformList(value)
            (value is ArrayNode) -> transformList(value)
            else -> mapOf(value.evalPath(path!!) to value)
        }
    }

    private fun transformList(values: Any?): Any {
        val items: List<Any?> = values.coerce() ?: emptyList()
        return items.associateBy { it.evalPath(path!!) }
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
            "slice" to SlicePipe(),
            "take" to TakePipe(),
            "takeLast" to TakeLastPipe()
        )
    )
        .transform("associateBy") { AssociateByPipe() }
}
