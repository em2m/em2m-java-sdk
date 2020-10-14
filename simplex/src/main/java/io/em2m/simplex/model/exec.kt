package io.em2m.simplex.model


// standard ops: log, httpRequest,

data class Exec(val op: String, val params: Map<String, Expr?> = emptyMap())

interface ExecHandler {
    fun call(context: ExprContext, op: String, params: Map<String, Any?>): Any?
}

class ExecExpr(val op: String, val handler: ExecHandler?, val paramExprs: Map<String, Expr?>, val map: Expr? = null) : Expr {

    override fun call(context: ExprContext): Any? {
        val contextExecs: ExecResolver? = context["execs"] as? ExecResolver
        val h = contextExecs?.findHandler(op) ?: handler
        val params = paramExprs.mapValues { it.value?.call(context) }
        val result = h?.call(context, op, params)
        return if (map != null) {
            map.call(context.plus("result" to result))
        } else result
    }
}

interface ExecResolver {
    fun findHandler(op: String): ExecHandler?
}

class BasicExecResolver(handlers: Map<String, (op: String) -> ExecHandler> = emptyMap()) : ExecResolver {

    private val handlers = HashMap<String, (op: String) -> ExecHandler>()

    private val delegates = ArrayList<ExecResolver>()

    init {
        this.handlers.putAll(handlers)
    }

    fun handler(key: String, handler: (op: String) -> ExecHandler): BasicExecResolver {
        handlers[key] = handler
        return this
    }

    fun delegate(delegate: ExecResolver): BasicExecResolver {
        delegates.add(delegate)
        return this
    }

    override fun findHandler(op: String): ExecHandler? {
        var result = handlers[op]?.invoke(op)
        for (delegate in delegates) {
            if (result != null) break
            result = delegate.findHandler(op)
        }
        return result
    }

}
