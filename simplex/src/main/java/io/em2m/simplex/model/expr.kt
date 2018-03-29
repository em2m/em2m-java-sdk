package io.em2m.simplex.model


interface PipeTransform {
    fun transform(value: Any?, context: ExprContext): Any?
    fun args(args: List<String>) {}
}

interface Part {
    fun call(context: ExprContext): Any?
}

data class KeyOnlyPipePart(val key: Key, val handler: KeyHandler) : Part {
    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        return handler.call(key, context)
    }
}

data class SingleTransformPipePart(val key: Key, val handler: KeyHandler, val transform: PipeTransform) : Part {

    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        val initial = handler.call(key, context)
        return transform.transform(initial, context)
    }
}

data class MultiTransformPipePart(val key: Key, val handler: KeyHandler, val transforms: List<PipeTransform> = emptyList()) : Part {

    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        val initial = handler.call(key, context)
        return transforms.fold(initial, { current, pipe -> pipe.transform(current, context) })
    }
}


data class ConstPart(val value: String) : Part {

    override fun call(context: ExprContext): Any? {
        return value
    }
}

interface Expr {
    fun call(context: ExprContext): Any?
}

data class SinglePartExpr(val part: Part) : Expr {
    override fun call(context: ExprContext): Any? {
        return part.call(context)
    }
}

data class TwoPartExpr(val first: Part, val second: Part) : Expr {
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

data class ThreePartExpr(val first: Part, val second: Part, val third: Part) : Expr {
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

data class MultiPartExpr(val parts: List<Part>) : Expr {

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

    val handlers = HashMap<String, (String) -> PipeTransform>()

    private val delegates = ArrayList<PipeTransformResolver>()

    init {
        handlers.forEach {
            this.handlers.put(it.key, { _ -> it.value })
        }
    }

    fun delegate(delegate: PipeTransformResolver): BasicPipeTransformResolver {
        delegates.add(delegate)
        return this
    }

    fun transform(key: String, transform: PipeTransform): BasicPipeTransformResolver {
        handlers.put(key, { _ -> transform })
        return this
    }

    fun transform(key: String, transform: (String) -> PipeTransform): BasicPipeTransformResolver {
        handlers.put(key, transform)
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
