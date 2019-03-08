package io.em2m.policy.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.em2m.simplex.model.Condition
import io.em2m.simplex.model.ConditionExpr
import io.em2m.simplex.model.ConstConditionExpr

enum class Effect { Allow, Deny }

data class Statement(val id: String? = null, val effect: Effect, val actions: List<String>,
                     @JsonFormat(with = arrayOf(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                     val resource: List<String> = emptyList(),
                     val condition: ConditionExpr = ConstConditionExpr(true))

data class Role(val id: String,
                val label: String = id,
                val policies: List<String> = emptyList(),
                val inherits: List<String> = emptyList(),
                val statements: List<Statement> = emptyList(),
                val condition: List<Condition> = emptyList())

data class Policy(val id: String,
                  val label: String, val statements: List<Statement>)

interface PolicySource {
    val policies: List<Policy>
    val roles: List<Role>
}
