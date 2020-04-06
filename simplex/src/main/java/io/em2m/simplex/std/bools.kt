package io.em2m.simplex.std

import io.em2m.simplex.Simplex
import io.em2m.simplex.model.*
import io.em2m.utils.coerce

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

class ConditionTransform(val simplex: Simplex) : PipeTransform {

    var condition: ConditionHandler? = null

    var params: List<String> = emptyList()

    override fun args(args: List<String>) {
        val op = args[0]
        condition = simplex.findConditionHandler(op)
        params = if (args.size > 1) {
            args.subList(1, args.size)
        } else emptyList()
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val values = params.map { simplex.eval(it, context) }
        return condition?.test(value, values) ?: false
    }

}

class BoolKeyHandler(val value: Boolean) : KeyHandler {

    override fun call(key: Key, context: ExprContext): Any? {
        return value
    }

}

val StandardBoolConditions = mapOf(
        "Bool" to Bool()
)

class BooleanLabelPipe : PipeTransform {
    var trueLabel: String? = null
    var falseLabel: String? = null
    var nullLabel: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty() && args.size >= 2) {
            trueLabel = args[0]
            falseLabel = args[1]
            if (args.size >= 3) {
                nullLabel = args[2]
            }
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when {
            value == true -> {
                trueLabel
            }
            value == false -> {
                falseLabel
            }
            nullLabel != null -> {
                nullLabel
            }
            else -> ""
        }
    }
}


object Bools {

    val conditions = BasicConditionResolver(StandardBoolConditions)

    val keys = BasicKeyResolver()
            .key(Key("Bool", "true")) { BoolKeyHandler(true) }
            .key(Key("Bool", "false")) { BoolKeyHandler(false) }

    fun pipes(simplex: Simplex): PipeTransformResolver {
        return BasicPipeTransformResolver()
                .transform("condition") { ConditionTransform(simplex) }
                .transform("cond") { ConditionTransform(simplex) }
                .transform("boolLabel") { BooleanLabelPipe() }
    }
}