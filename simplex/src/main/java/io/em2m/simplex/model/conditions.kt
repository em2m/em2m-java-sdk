package io.em2m.simplex.model

interface ConditionHandler {
    fun test(keyValue: Any?, conditionValue: Any?): Boolean
}

open class Not(val delegate: ConditionHandler) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        return !delegate.test(keyValue, conditionValue)
    }
}

interface ConditionResolver {
    fun getCondition(condition: String): ConditionHandler?
}

class BasicConditionResolver(conditions: Map<String, ConditionHandler> = emptyMap()) : ConditionResolver {

    private val handlers = HashMap<String, ConditionHandler>()

    private val delegates = ArrayList<ConditionResolver>()

    init {
        handlers.putAll(conditions)
    }

    fun condition(key: String, handler: ConditionHandler): BasicConditionResolver {
        handlers[key] = handler
        return this
    }

    fun delegate(delegate: ConditionResolver): BasicConditionResolver {
        delegates.add(delegate)
        return this
    }

    override fun getCondition(condition: String): ConditionHandler? {
        var result = handlers[condition]
        for (delegate in delegates) {
            if (result != null) break
            result = delegate.getCondition(condition)
        }
        return result
    }
}

interface ConditionExpr : Expr {
    override fun call(context: ExprContext): Boolean
}

class ConstConditionExpr(val value: Boolean) : ConditionExpr {
    override fun call(context: ExprContext): Boolean {
        return value
    }
}

class SingleConditionExpr(val condition: String, val handler: ConditionHandler, val first: Expr, val second: Expr) : ConditionExpr {

    override fun call(context: ExprContext): Boolean {

        val firstVal = first.call(context)
        val secondVal = second.call(context)

        return handler.test(firstVal, secondVal)
    }
}

class NotConditionExpr(val conditions: List<ConditionExpr>) : ConditionExpr {

    override fun call(context: ExprContext): Boolean {
        conditions.forEach {
            if (it.call(context)) return false
        }
        return true
    }
}

class OrConditionExpr(val conditions: List<ConditionExpr>) : ConditionExpr {

    override fun call(context: ExprContext): Boolean {
        conditions.forEach {
            if (it.call(context)) return true
        }
        return false
    }
}

class AndConditionExpr(val conditions: List<ConditionExpr>) : ConditionExpr {

    override fun call(context: ExprContext): Boolean {
        conditions.forEach {
            if (!it.call(context)) return false
        }
        return true
    }
}