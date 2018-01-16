package io.em2m.simplex.model


interface PipeTransform {
    fun transform(value: Any?): Any?
    fun args(args: List<String>) {}
}

interface PipeTransformResolver {
    fun find(key: String): PipeTransform?
}

interface Part {
    fun call(context: ExprContext): Any?
}

data class PipePart(val key: Key, val handler: KeyHandler, val transforms: List<PipeTransform> = emptyList()) : Part {

    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        val initial = handler.call(key, context)
        return transforms.fold(initial, { initial, pipe -> pipe.transform(initial) })
    }
}

data class ConstPart(val value: String) : Part {

    override fun call(context: ExprContext): Any? {
        return value
    }
}

data class Expr(val parts: List<Part>) {

    fun call(context: ExprContext): Any? {
        val values = parts.mapNotNull { it.call(context) }
        return if (values.size == 1) {
            values.first()
        } else {
            values.joinToString("")
        }
    }
}