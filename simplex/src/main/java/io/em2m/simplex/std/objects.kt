package io.em2m.simplex.std

import com.fasterxml.jackson.databind.node.ArrayNode
import io.em2m.simplex.evalPath
import io.em2m.simplex.model.*
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
            (value is ArrayNode) -> transformList(value)
            else -> value.evalPath(path!!)
        }
    }

    private fun transformList(values: Any?): Any? {
        val items: List<Any?> = values.coerce() ?: emptyList()
        return items.mapNotNull {
            it.evalPath(path!!)
        }
    }

}

class PathByPipe() : PipeTransform {

    private var pathExpr: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            pathExpr = args[0].trim()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        if (pathExpr == null) {
            return null
        }
        val path = context.evalPath(pathExpr!!)?.toString()

        return when {
            path.isNullOrEmpty() -> null
            (value is List<*>) -> transformList(value, path)
            (value is Array<*>) -> transformList(value, path)
            (value is ArrayNode) -> transformList(value, path)
            else -> value.evalPath(path)
        }
    }

    private fun transformList(values: Any?, path: String): Any {
        val items: List<Any?> = values.coerce() ?: emptyList()
        return items.mapNotNull {
            it.evalPath(path)
        }
    }

}


class EntriesPipe : PipeTransform {


    override fun transform(value: Any?, context: ExprContext): Any? {
        return when {
            (value is List<*>) -> transformObject(value.firstOrNull())
            (value is Array<*>) -> transformObject(value.firstOrNull())
            (value is ArrayNode) -> transformObject(value.firstOrNull())
            else -> transformObject(value)
        }
    }

    private fun transformObject(value: Any?): Any {
        val map: Map<String, Any?> = value.coerce() ?: emptyMap()
        return map.entries.map { mapOf("key" to it.key, "value" to it.value) }
    }

}

class PairHandler : ExecHandler {

    override fun call(context: ExprContext, op: String, params: Map<String, Any?>): Any? {
        val key = params["key"]
        val value = params["value"]
        return mapOf(key to value)
    }
}

object Objects {
    val pipes = BasicPipeTransformResolver()
        .transform("path") { PathPipe() }
        .transform("pathBy") { PathByPipe() }
        .transform("entries", EntriesPipe())
    val execs = BasicExecResolver()
        .handler("object:pair") { PairHandler() }

}
