package io.em2m.simplex.std

import io.em2m.simplex.model.*
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class NumberPipe : PipeTransform {

    private val format: DecimalFormat = DecimalFormat().apply { maximumFractionDigits = 3 }

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            val fractionSize = args[0].toInt()
            format.maximumFractionDigits = fractionSize
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value != null) {
            if (value is Number) {
                format.format(value)
            } else ""
        } else null
    }

}

class RoundPipe : PipeTransform {

    private var precision: Int = 0

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            precision = Integer.parseInt(args[0])
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is Number) {
            BigDecimal(value.toDouble()).setScale(precision, RoundingMode.HALF_UP)
        } else value
    }

}


class MultiplyPipe : PipeTransform {

    private var multiplier: Double = 0.0

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            multiplier = args[0].toDouble()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return try {
            value.toString().toDouble() * multiplier
        } catch (ex: Exception) {
            null
        }
    }
}

class AddPipe : PipeTransform {

    private var addend: Double = 0.0

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            addend = args[0].toDouble()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return try {
            value.toString().toDouble() + addend
        } catch (ex: Exception) {
            null
        }

    }
}


class RandomKey : KeyHandler {

    override fun call(key: Key, context: ExprContext): Any? {
        return Math.random()
    }
}

fun toNumber(value: Any?): Number? {
    return when (value) {
        is Number -> value
        is String -> value.toLongOrNull() ?: value.toDoubleOrNull()
        else -> null
    }
}

fun compareNumbers(n1: Number?, n2: Number?): Int {
    return when {
        (n1 == n2) -> 0
        (n2 == null) -> 1
        (n1 == null) -> -1
        (n1 is Float) -> n1.compareTo(n2.toFloat())
        (n1 is Double) -> n1.compareTo(n2.toDouble())
        (n1 is Int) -> n1.compareTo(n2.toInt())
        (n1 is Long) -> n1.compareTo(n2.toLong())
        (n1 is Short) -> n1.compareTo(n2.toShort())
        else -> n1.toDouble().compareTo(n2.toDouble())
    }
}

open class SingleNumberHandler(private val op: (Number?, Number?) -> Boolean) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyNumber = if (keyValue is List<*>) {
            toNumber(keyValue[0])
        } else toNumber(keyValue)
        val valueNumber = if (conditionValue is List<*>) {
            toNumber(conditionValue[0])
        } else toNumber(conditionValue)

        return op(keyNumber, valueNumber)
    }
}

class NumberEquals : SingleNumberHandler({ k, v -> k == v })
class NumberGreaterThan : SingleNumberHandler({ k, v -> compareNumbers(k, v) > 0 })
class NumberGreaterThanEquals : SingleNumberHandler({ k, v -> compareNumbers(k, v) >= 0 })
class NumberLessThan : SingleNumberHandler({ k, v -> compareNumbers(k, v) < 0 })
class NumberLessThanEquals : SingleNumberHandler({ k, v -> compareNumbers(k, v) <= 0 })

val StandardNumberConditions = mapOf(
        "NumberEquals" to NumberEquals(),
        "NumberGreaterThan" to NumberGreaterThan(),
        "NumberGreaterThanEquals" to NumberGreaterThanEquals(),
        "NumberLessThan" to NumberLessThan(),
        "NumberLessThanEquals" to NumberLessThanEquals()
)

object Numbers {

    val pipes = BasicPipeTransformResolver()
            .transform("number") { _ -> NumberPipe() }
            .transform("round") { _ -> RoundPipe() }
            .transform("multiply") { _ -> MultiplyPipe() }
            .transform("add") { _ -> AddPipe() }

    val keys = BasicKeyResolver()
            .key(Key("Math", "PI")) { _ -> ConstKeyHandler(Math.PI) }
            .key(Key("Math", "random")) { _ -> RandomKey() }

    val conditions = BasicConditionResolver(StandardNumberConditions)

}