package io.em2m.simplex.model


data class Condition(val op: String, val key: String, val value: List<String>)

interface ConditionHandler {
    fun test(keyValue: Any?, conditionValue: Any?): Boolean
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
        handlers.put(key, handler)
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

class ConditionExpr(val condition: String, val handler: ConditionHandler, val first: Expr, val second: Expr) {

    fun call(context: ExprContext): Boolean {

        val firstVal = first.call(context)
        val secondVal = second.call(context)

        return handler.test(firstVal, secondVal)
    }

}