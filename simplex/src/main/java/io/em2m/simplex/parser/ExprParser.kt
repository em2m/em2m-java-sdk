package io.em2m.simplex.parser

import io.em2m.simplex.model.*

class ExprParser(val keyResolver: KeyResolver, val pipeTransformResolver: PipeTransformResolver) {

    val pipeExpr = Regex("\\$\\{([^}]*)}")

    val BLANK = ConstPart("")

    fun parse(expr: String): Expr {
        val text = expr.split(pipeExpr).map(::ConstPart)
        val pipes = pipeExpr.findAll(expr).toList().map { parsePipe(it.groupValues[1]) }
        val parts = merge(text, pipes).filter { it != BLANK }
        return Expr(parts)
    }

    fun merge(splits: List<Part>, patterns: List<Part>): List<Part> {
        val results = ArrayList<Part>()
        (0 until patterns.size).forEach { i ->
            results.add(splits[i])
            results.add(patterns[i])
        }
        results.add(splits[splits.size - 1])
        return results
    }

    fun parsePipe(text: String): PipePart {
        val splits = text.split("|")
        val key = Key.parse(splits.first().trim())
        val handler = requireNotNull(keyResolver.find(key), { "Key ($key) not found" })

        val transforms: List<PipeTransform> = if (splits.size > 1) {
            splits.subList(1, splits.size).map { xformExpr ->
                val xformParts = xformExpr.split(":")
                require(xformParts.isNotEmpty(), { "Invalid pipe expressions" })
                val pipeName = xformParts.first()
                val args = xformParts.drop(1)
                requireNotNull(pipeTransformResolver.find(pipeName.trim())).apply { args(args) }
            }
        } else emptyList()

        return PipePart(key, handler, transforms)
    }


}