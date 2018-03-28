package io.em2m.policy.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.em2m.simplex.model.Condition
import io.em2m.simplex.parser.ConditionsDeserializer

enum class Effect { Allow, Deny }

data class Statement(val id: String? = null, val effect: Effect, val actions: List<String>,
                     @JsonFormat(with = arrayOf(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                     val resource: List<String> = emptyList(),
                     @JsonDeserialize(using = ConditionsDeserializer::class)
                     val condition: List<Condition> = emptyList())

data class Role(val id: String,
                val label: String, val policies: List<String>,
                val inherits: List<String> = emptyList(),
                val condition: List<Condition> = emptyList())

data class Policy(val id: String,
                  val label: String, val statements: List<Statement>)
