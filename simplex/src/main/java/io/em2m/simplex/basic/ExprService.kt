package io.em2m.simplex.basic

import io.em2m.simplex.model.*
import io.em2m.simplex.parser.ExprParser
import io.em2m.simplex.pipes.CapitalizePipe
import io.em2m.simplex.pipes.UpperCasePipe
import org.slf4j.LoggerFactory


class ExprService(val keyResolver: KeyResolver, val conditionResolver: ConditionResolver) {

    var LOG = LoggerFactory.getLogger(javaClass)

    val pipeResolver = BasicPipeTransformResolver(mapOf("upperCase" to UpperCasePipe(), "capitalize" to CapitalizePipe()))

    val parser = ExprParser(keyResolver, pipeResolver)

    fun testConditions(conditions: List<Condition>, context: ExprContext): Boolean {
        var result = true
        conditions.forEach {
            LOG.debug("condition: ${it.op}")
            val conditionHandler = getCondition(it.op)
            val keyValue = getKeyValue(it.key, context)
            val values = it.value.map { getValue(it, context) }
            if (!conditionHandler.test(keyValue, values)) {
                result = false
            }
        }
        return result
    }

    private fun getCondition(op: String): ConditionHandler {
        return requireNotNull(conditionResolver.getCondition(op))
    }

    private fun getKeyValue(key: String, context: ExprContext): Any? {
        val key = Key.parse(key)
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(key) ?: keyResolver.find(key)
        return handler?.call(key, context)
    }

    fun getValue(value: String, context: ExprContext): Any? {
        return parser.parse(value).call(context)
    }

}