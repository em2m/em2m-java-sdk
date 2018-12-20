package io.em2m.rules

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import io.em2m.simplex.model.ConditionExpr
import io.em2m.simplex.model.ExprContext
import io.em2m.simplex.model.KeyResolver
import java.util.concurrent.atomic.AtomicInteger


data class Assertion(val key: String, val value: List<Any?>)

data class Assertions(val assertions: List<Assertion>) {

    val keys = assertions.map { it.key }.toSet()

}

data class Rule(
        val id: String? = null,
        @JsonProperty("match")
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED])
        val match: List<ConditionExpr> = emptyList(),
        @JsonProperty("do")
        val doRules: List<Rule> = emptyList(),
        @JsonProperty("when")
        val whenRules: List<Rule> = emptyList(),
        @JsonProperty("assert")
        val assertions: Assertions = Assertions(emptyList())) {

    val keys: Set<String>

    init {
        keys = doRules.flatMap { it.keys }
                .plus(whenRules.flatMap { it.keys }).toSet()
                .plus(assertions.keys)
    }

}

open class RuleContext(
        val exprContext: ExprContext = emptyMap(),
        val keys: KeyResolver? = null)


interface RuleEngine {
    fun test(context: RuleContext, assertion: Assertion): Boolean
    fun values(context: RuleContext, key: String): List<Any?>
}
