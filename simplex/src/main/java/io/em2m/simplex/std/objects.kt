package io.em2m.simplex.std

import io.em2m.simplex.evalPath
import io.em2m.simplex.model.BasicPipeTransformResolver
import io.em2m.simplex.model.ExprContext
import io.em2m.simplex.model.PipeTransform
import io.em2m.utils.coerce

class PathPipe() : PipeTransform {

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
            else -> value.evalPath(path!!)
        }
    }

    fun transformList(values: Any?): Any? {
        val items: List<Any?> = values.coerce() ?: emptyList()
        if (items.isNullOrEmpty()) return null
        val result: MutableList<Any?> = mutableListOf()
        items.forEach{
            val obj: Map<String, Any?> = it.coerce() ?: emptyMap()
            if (!obj.isNullOrEmpty()) {
                val value = obj.evalPath(path!!)
                if (value != null) result.add(value)
            }
        }
        return result
    }

}

object Objects {
    val pipes = BasicPipeTransformResolver()
            .transform("path") { PathPipe() }
}