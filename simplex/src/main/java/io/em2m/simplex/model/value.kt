package io.em2m.simplex.model


interface PipeTransform {
    fun transform(value: Any?, context: ExprContext): Any?
    fun args(args: List<String>) {}
}

interface Part {
    fun call(context: ExprContext): Any?
}

interface PipePart : Part {
    val key: Key
    val handler: KeyHandler?
    val transforms: List<PipeTransform>
}

data class KeyOnlyPipePart(override val key: Key, override val handler: KeyHandler?) : PipePart {

    override val transforms = emptyList<PipeTransform>()

    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        return requireNotNull(handler) { "Handler not found for $key" }.call(key, context)
    }
}

data class SingleTransformPipePart(override val key: Key, override val handler: KeyHandler?, val transform: PipeTransform) : PipePart {

    override val transforms = listOf(transform)

    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        val initial = requireNotNull(handler) { "Handler not found for $key" }.call(key, context)
        return transform.transform(initial, context)
    }
}

data class MultiTransformPipePart(override val key: Key, override val handler: KeyHandler?, override val transforms: List<PipeTransform> = emptyList()) : PipePart {

    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        val initial = requireNotNull(handler) { "Handler not found for $key" }.call(key, context)
        return transforms.fold(initial) { current, pipe -> pipe.transform(current, context) }
    }
}


data class ConstPart(val value: Any?) : Part {

    override fun call(context: ExprContext): Any? {
        return value
    }
}

data class ExprPart(val expr: Expr?) : Part {

    override fun call(context: ExprContext): Any? {
        return expr?.call(context)
    }
}


interface ValueExpr : Expr {
    val parts: List<Part>
}

data class ConstExpr(val value: Any?) : Expr {

    override fun call(context: ExprContext): Any? {
        return value
    }

}

data class ConstValueExpr(val value: Any?) : ValueExpr {

    override val parts: List<Part> = listOf(ConstPart(value))

    override fun call(context: ExprContext): Any? {
        return value
    }

}


data class SinglePartExpr(val part: Part) : ValueExpr {

    override val parts = listOf(part)

    override fun call(context: ExprContext): Any? {
        return part.call(context)
    }
}

data class TwoPartExpr(val first: Part, val second: Part) : ValueExpr {

    override val parts = listOf(first, second)

    override fun call(context: ExprContext): Any? {
        val builder = StringBuilder()
        val v1 = first.call(context)
        val v2 = second.call(context)
        if (v1 != null) {
            builder.append(v1)
        }
        if (v2 != null) {
            builder.append(v2)
        }
        return builder.toString()
    }
}

data class ThreePartExpr(val first: Part, val second: Part, val third: Part) : ValueExpr {

    override val parts = listOf(first, second, third)

    override fun call(context: ExprContext): Any? {
        val builder = StringBuilder()
        val v1 = first.call(context)
        val v2 = second.call(context)
        val v3 = third.call(context)
        if (v1 != null) {
            builder.append(v1)
        }
        if (v2 != null) {
            builder.append(v2)
        }
        if (v3 != null) {
            builder.append(v3)
        }
        return builder.toString()
    }
}

data class MultiPartExpr(override val parts: List<Part>) : ValueExpr {

    override fun call(context: ExprContext): Any? {
        val builder = StringBuilder()
        parts.forEach { part ->
            val value = part.call(context)
            if (value != null) {
                builder.append(value)
            }
        }
        return builder.toString()
    }
}

interface PipeTransformResolver {
    fun find(key: String): PipeTransform?
}

class BasicPipeTransformResolver(handlers: Map<String, PipeTransform> = emptyMap()) : PipeTransformResolver {

    private val handlers = HashMap<String, (String) -> PipeTransform>()

    private val delegates = ArrayList<PipeTransformResolver>()

    init {
        handlers.forEach {
            this.handlers[it.key] = { _ -> it.value }
        }
    }

    fun delegate(delegate: PipeTransformResolver): BasicPipeTransformResolver {
        delegates.add(delegate)
        return this
    }

    fun transform(key: String, transform: PipeTransform): BasicPipeTransformResolver {
        handlers[key] = { _ -> transform }
        return this
    }

    fun transform(key: String, transform: (String) -> PipeTransform): BasicPipeTransformResolver {
        handlers[key] = transform
        return this
    }

    override fun find(key: String): PipeTransform? {
        var result = handlers[key]?.invoke(key)
        for (delegate in delegates) {
            if (result != null) break
            result = delegate.find(key)
        }
        return result
    }

}
