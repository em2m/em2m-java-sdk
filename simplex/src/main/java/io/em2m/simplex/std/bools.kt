package io.em2m.simplex.std

import io.em2m.simplex.model.BasicConditionResolver
import io.em2m.simplex.model.ConditionHandler
import io.em2m.utils.coerce
import io.em2m.utils.coerceNonNull

class Bool : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyBool: Boolean? = (if (keyValue is List<*>) {
            keyValue.first()
        } else keyValue).coerce()
        val valueBool: Boolean? = (if (conditionValue is List<*>) {
            conditionValue.first()
        } else {
            conditionValue
        }).coerce()
        return if (keyBool == null || valueBool == null) {
            false
        } else {
            keyBool == valueBool
        }
    }

}

val StandardBoolConditions = mapOf(
        "Bool" to Bool()
)

object Bools {
    val conditions = BasicConditionResolver(StandardBoolConditions)
}