package io.em2m.simplex

import io.em2m.simplex.model.*
import io.em2m.simplex.parser.ExprParser
import io.em2m.simplex.std.Bools
import io.em2m.simplex.std.I18n
import io.em2m.simplex.std.Numbers
import io.em2m.simplex.std.Strings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class Simplex {

    private val keys = BasicKeyResolver()
            .delegate(Numbers.keys)

    private val pipes = BasicPipeTransformResolver()
            .delegate(Numbers.pipes)
            .delegate(Strings.pipes)
            .delegate(I18n.pipes)

    private val conditions = BasicConditionResolver()
            .delegate(Strings.conditions)
            .delegate(Numbers.conditions)
            .delegate(Bools.conditions)

    val cache: ConcurrentMap<String, Expr> = ConcurrentHashMap()

    val parser = ExprParser(keys, pipes)

    fun keys(delegate: KeyResolver): Simplex {
        keys.delegate(delegate)
        cache.clear()
        return this
    }

    fun pipes(delegate: PipeTransformResolver): Simplex {
        pipes.delegate(delegate)
        cache.clear()
        return this
    }

    fun conditions(delegate: ConditionResolver): Simplex {
        conditions.delegate(delegate)
        cache.clear()
        return this
    }

    fun testConditions(conditions: List<Condition>, context: ExprContext): Boolean {
        var result = true
        conditions.forEach {
            val conditionHandler = getCondition(it.op)
            val keyValue = getKeyValue(it.key, context)
            val values = it.value.map { eval(it, context) }
            if (!conditionHandler.test(keyValue, values)) {
                result = false
            }
        }
        return result
    }

    private fun getCondition(op: String): ConditionHandler {
        return requireNotNull(conditions.getCondition(op))
    }

    private fun getKeyValue(key: String, context: ExprContext): Any? {
        val parsedKey = Key.parse(key)
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(parsedKey) ?: keys.find(parsedKey)
        return handler?.call(parsedKey, context)
    }

    fun eval(expr: String, context: ExprContext): Any? {
        val parsed = cache.getOrPut(expr, { parser.parse(expr) })
        // safey measure until we implement an extensible caching API
        if (cache.size > 1000) {
            cache.clear()
        }
        return parsed.call(context)
    }

}