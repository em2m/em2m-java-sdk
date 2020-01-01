package io.em2m.simplex.model


// standard ops: log, httpRequest,

data class Exec(
        val op: String,
        val config: Map<String, String> = emptyMap(),
        val params: Map<String, String> = emptyMap())

interface ExecHandler {
    fun configure(config: Map<String, Any?>)
    fun call(context: ExprContext, op: String, params: Map<String, Any?>): ExecResult
}

data class ExecResult(val value: Any? = null)

interface ExecResolver {
    fun findHandler(exec: Exec): ExecHandler?
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

    override fun findHandler(exec: Exec): ExecHandler? {
        var result = handlers[exec.op]?.invoke(exec.op)
        for (delegate in delegates) {
            if (result != null) break
            result = delegate.findHandler(exec)
        }
        return result
    }

}

class ExecExpr(val op: String, val handler: ExecHandler, val config: Map<String, Any?>, val paramExprs: Map<String, Expr>) {

    fun call(context: ExprContext): ExecResult {

        val params = paramExprs.mapValues { it.value.call(context) }

        return handler.call(context, op, params)
    }
}