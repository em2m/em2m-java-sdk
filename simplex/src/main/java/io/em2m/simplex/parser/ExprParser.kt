package io.em2m.simplex.parser

import io.em2m.simplex.model.*

class ExprParser(private val keyResolver: KeyResolver, private val pipeTransformResolver: PipeTransformResolver) {

    private val pipeExpr = Regex("\\$\\{([^}]*)}")

    fun parse(expr: String): ValueExpr {
        val text = expr.split(pipeExpr).map(::ConstPart)
        val pipes = pipeExpr.findAll(expr).toList().map { parsePipe(it.groupValues[1]) }
        val parts = merge(text, pipes).filter { it != BLANK }
        return when (parts.size) {
            1 -> SinglePartExpr(parts[0])
            2 -> TwoPartExpr(parts[0], parts[1])
            3 -> ThreePartExpr(parts[0], parts[1], parts[2])
            else -> MultiPartExpr(parts)
        }
    }

    private fun merge(splits: List<Part>, patterns: List<Part>): List<Part> {
        val results = ArrayList<Part>()
        (0 until patterns.size).forEach { i ->
            results.add(splits[i])
            results.add(patterns[i])
        }
        results.add(splits[splits.size - 1])
        return results
    }

    private fun parsePipe(text: String): Part {
        val splits = text.split('|')
        val key = Key.parse(splits.first().trim())
        val handler = keyResolver.find(key)

        val transforms: List<PipeTransform> = if (splits.size > 1) {
            splits.subList(1, splits.size).map { xformExpr ->
                val xformParts = xformExpr.replace("\\:", "__COLON__").split(':').map {
                    it.replace("__COLON__", ":")
                }
                require(xformParts.isNotEmpty()) { "Invalid pipe expressions" }
                val pipeName = xformParts.first().trim()
                val args = xformParts.drop(1)
                requireNotNull(pipeTransformResolver.find(pipeName)) { "Unknown pipe: $pipeName" }.apply { args(args) }
            }
        } else emptyList()

        return when (transforms.size) {
            0 -> KeyOnlyPipePart(key, handler)
            1 -> SingleTransformPipePart(key, handler, transforms[0])
            else -> MultiTransformPipePart(key, handler, transforms)
        }
    }

    companion object {
        private val BLANK = ConstPart("")
    }

}