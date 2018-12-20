package io.em2m.rules.basic

import io.em2m.rules.Assertion
import io.em2m.rules.RuleEngine
import io.em2m.rules.Rule
import io.em2m.rules.RuleContext

class BasicRuleEngine(val rules: List<Rule>) : RuleEngine {

    override fun test(context: RuleContext, assertion: Assertion): Boolean {
        val values = values(context, assertion.key)
        assertion.value.forEach {
            if (!values.contains(it)) return false
        }
        return true
    }

    override fun values(context: RuleContext, key: String): List<Any?> {
        println("Checking value for $key")
        val result = values(context, key, rules)
        println("value of $key = $result")
        return result
    }

    fun values(context: RuleContext, key: String, rules: List<Rule>, handleNegations: Boolean = false): List<Any?> {
        val values = rules.flatMap { values(context, key, it) }
        if (values.isNotEmpty() && handleNegations) {
            // val neg =
        }
        return values
    }

    fun values(context: RuleContext, key: String, rule: Rule): List<Any?> {
        val skip = !rule.keys.contains(key)
        val result = if (!skip && matches(context, rule)) {
            rule.assertions.assertions
                    .filter { it.key == key }
                    .flatMap { it.value }
                    .plus(values(context, key, rule.doRules))
        } else {
            emptyList()
        }
        return result
    }

    private fun matches(context: RuleContext, rule: Rule): Boolean {
        rule.match.forEach { condition ->
            if (!condition.call(context.exprContext)) {
                return false
            }
        }
        return true
    }

}