package io.em2m.simplex.basic

import io.em2m.simplex.model.*
import io.em2m.simplex.parser.ExprParser
import org.slf4j.LoggerFactory


class ExprService(private val keyResolver: KeyResolver, private val pipeResolver: PipeTransformResolver, private val conditionResolver: ConditionResolver) {

    var LOG = LoggerFactory.getLogger(javaClass)

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
        val parsedKey = Key.parse(key)
        val contextKeys: KeyResolver? = context["keys"] as? KeyResolver
        val handler = contextKeys?.find(parsedKey) ?: keyResolver.find(parsedKey)
        return handler?.call(parsedKey, context)
    }

    fun getValue(value: String, context: ExprContext): Any? {
        return parser.parse(value).call(context)
    }

}