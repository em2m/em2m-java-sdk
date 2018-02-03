package io.em2m.simplex.model


interface PipeTransform {
    fun transform(value: Any?): Any?
    fun args(args: List<String>) {}
}

interface Part {
    fun call(context: ExprContext): Any?
}

data class PipePart(val key: Key, val handler: KeyHandler, val transforms: List<PipeTransform> = emptyList()) : Part {

    override fun call(context: ExprContext): Any? {
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: handler
        val initial = handler.call(key, context)
        return transforms.fold(initial, { current, pipe -> pipe.transform(current) })
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
