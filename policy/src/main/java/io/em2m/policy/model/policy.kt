package io.em2m.policy.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.em2m.simplex.model.ConditionExpr
import io.em2m.simplex.model.ConstConditionExpr

enum class Effect { Allow, Deny }

data class Statement(val id: String? = null,
                     val effect: Effect = Effect.Allow,
                     val actions: List<String> = emptyList(),
                     val allow: Map<String, List<Any>> = emptyMap(),
                     val deny: Map<String, List<Any>> = emptyMap(),
                     @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED])
                     val resource: List<String> = emptyList(),
                     val condition: ConditionExpr = ConstConditionExpr(true))

data class Role(val id: String,
                val label: String = id,
                val summary: String? = null,
                val policies: List<String> = emptyList(),
                val inherits: List<String> = emptyList(),
                val statements: List<Statement> = emptyList(),
                val priority: Int = 0,
                val condition: ConditionExpr = ConstConditionExpr(false))

data class Policy(val id: String,
                  val label: String,
                  val statements: List<Statement>)

interface PolicySource {
    val policies: List<Policy>
    val roles: List<Role>
}
