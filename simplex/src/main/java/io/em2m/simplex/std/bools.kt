package io.em2m.simplex.std

import com.scaleset.utils.Coerce
import io.em2m.simplex.model.BasicConditionResolver
import io.em2m.simplex.model.ConditionHandler

class Bool : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyBool = Coerce.toBoolean(keyValue)
        val valueBool = Coerce.toBoolean(conditionValue)
        return keyBool == valueBool
    }

}

val StandardBoolConditions = mapOf(
        "Bool" to Bool()
)

object Bools {
    val conditions = BasicConditionResolver(StandardBoolConditions)
}