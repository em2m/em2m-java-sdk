package io.em2m.simplex.std

import io.em2m.simplex.model.BasicConditionResolver
import io.em2m.simplex.model.ConditionHandler
import io.em2m.utils.coerce

class Bool : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyBool: Boolean? = keyValue.coerce()
        val valueBool: Boolean? = conditionValue.coerce()
        return keyBool == valueBool
    }

}

val StandardBoolConditions = mapOf(
        "Bool" to Bool()
)

object Bools {
    val conditions = BasicConditionResolver(StandardBoolConditions)
}