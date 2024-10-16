package io.em2m.simplex.std

import com.fasterxml.jackson.databind.node.ArrayNode
import io.em2m.simplex.evalPath
import io.em2m.simplex.model.*
import io.em2m.utils.coerce
import java.math.BigDecimal
import java.math.RoundingMode

class NotNullPipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.filterNotNull()
            is Array<*> -> value.filterNotNull()
            is ArrayNode -> value.firstOrNull()
            is Set<*> -> value.filterNotNull()
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
            is Set<*> -> value.reversed()
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
            is Set<*> -> value.filterNotNull()
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
            is Set<*> -> value.filterNot { it?.toString().isNullOrBlank() }
            else -> value
        }
    }
}

class FilterPipe : PipeTransform {
    private var path: String = ""
    private var targetVal: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            path = args[0]
            targetVal = args[1].trim()
        }
    }

    override fun transform(value: Any?, context: ExprContext): List<Any> {
        val convertedList: List<Any> = value?.coerce() ?: emptyList()
        return convertedList.filter { targetVal == it.evalPath(path) }
    }
}

class FirstPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.firstOrNull()
            is Array<*> -> value.firstOrNull()
            is ArrayNode -> value.firstOrNull()
            is Set<*> -> value.firstOrNull()
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
            is Set<*> -> value.find { !it?.toString().isNullOrBlank() }
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
            is Set<*> -> value.lastOrNull()
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
            is Set<*> -> value.findLast { !it?.toString().isNullOrBlank() }
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
            is Set<*> -> value.size
            else -> null
        }
    }
}

class MaxNumPipe : PipeTransform {

    private var precision: Int = 1
    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            precision = Integer.parseInt(args[0].trim())
        }
    }
    override fun transform(value: Any?, context: ExprContext): Any? {
        val convertedList: Array<Number>? = when (value) {
            is List<*> -> value.coerce()
            is Array<*> -> value.coerce()
            is ArrayNode -> value.coerce()
            is String -> value.toString().split(",").coerce()
            is Set<*> -> value.coerce()
            else -> null
        }
        val values: MutableList<BigDecimal> = mutableListOf()
        convertedList?.forEach {
            values.add(BigDecimal(it.toDouble()).setScale(precision, RoundingMode.HALF_UP))
        }
        return if (values.isNotEmpty()) {
            values.maxOrNull()?.toDouble()
        } else null
    }
}

class TakePipe : PipeTransform {
    var n = 0

    override fun args(args: List<String>) {
        n = args.firstOrNull()?.toInt() ?: 0
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.take(n)
            is Array<*> -> value.take(n)
            is ArrayNode -> value.take(n)
            is String -> value.take(n)
            is Set<*> -> value.take(n)
            else -> null
        }
    }
}

class TakeLastPipe : PipeTransform {
    var n = 0

    override fun args(args: List<String>) {
        n = args.firstOrNull()?.toInt() ?: 0
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.takeLast(n)
            is Array<*> -> value.takeLast(n)
            is ArrayNode -> convertToList(value)?.takeLast(n)
            is String -> value.takeLast(n)
            is Set<*> -> value.toList().takeLast(n)
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
            is Set<*> -> value.toList().slice(range)
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
            (value is Set<*>) -> transformList(value)
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
            "filter" to FilterPipe(),
            "filterNotNull" to FilterNotNullPipe(),
            "filterNotBlank" to FilterNotBlankPipe(),
            "first" to FirstPipe(),
            "firstNotBlank" to FirstNotBlankPipe(),
            "last" to LastPipe(),
            "lastNotBlank" to LastNotBlankPipe(),
            "size" to SizePipe(),
            "maxNum" to MaxNumPipe()
        )
    )
        .transform("associateBy") { AssociateByPipe() }
        .transform("take") { TakePipe() }
        .transform("slice") { SlicePipe() }
        .transform("takeLast") { TakeLastPipe() }
}
