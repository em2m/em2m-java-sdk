package io.em2m.simplex.std

import com.fasterxml.jackson.databind.node.ArrayNode
import io.em2m.simplex.model.*
import io.em2m.utils.coerce
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class NumberPipe : PipeTransform {

    private val format: DecimalFormat = DecimalFormat().apply { maximumFractionDigits = 3 }

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            val fractionSize = args[0].trim().toInt()
            format.maximumFractionDigits = fractionSize
            format.minimumFractionDigits = fractionSize
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
            precision = Integer.parseInt(args[0].trim())
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> ->  value.map { it?.round()}
            is Array<*> -> value.map { it?.round() }
            is ArrayNode -> value.map { it?.round() }
            else -> value?.round()
        }
    }

    private fun Any.round(): Any? {
        return if (this is Number) {
            BigDecimal(this.toDouble()).setScale(precision, RoundingMode.HALF_UP)
        } else this
    }

}


class TimesPIpe : PipeTransform {

    private var multiplier: Double = 0.0

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            multiplier = args[0].toDouble()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return try {
            value.coerce<Double>()?.times(multiplier)
        } catch (ex: Exception) {
            null
        }
    }
}

class DivPipe : PipeTransform {

    private var divisor: Double = 0.0

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            divisor = args[0].toDouble()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return try {
            value.coerce<Double>()?.div(divisor)
        } catch (ex: Exception) {
            null
        }
    }
}

class PlusPipe : PipeTransform {

    private var addend: Double = 0.0

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            addend = args[0].toDouble()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return try {
            value.coerce<Double>()?.plus(addend)
        } catch (ex: Exception) {
            null
        }

    }
}

class MinusPipe : PipeTransform {

    private var addend: Double = 0.0

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            addend = args[0].toDouble()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return try {
            value.coerce<Double>()?.minus(addend)
        } catch (ex: Exception) {
            null
        }

    }
}

class MaxPipe : PipeTransform {

    private var max: Double? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            max = args.first().trimEnd().coerce()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val maxValue = max
        return if (value is Number && maxValue != null) {
            if (value.toDouble() > maxValue) {
                maxValue
            } else value
        } else value
    }
}

class MinPipe : PipeTransform {

    private var min: Double? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            min = args.first().trimEnd().coerce()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val minValue = min
        return if (value is Number && minValue != null) {
            if (value.toDouble() < minValue) {
                minValue
            } else value
        } else value
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

class NumberEquals : SingleNumberHandler({ k, v -> compareNumbers(k, v) == 0 })
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
            .transform("number") { NumberPipe() }
            .transform("round") { RoundPipe() }
            .transform("max") { MaxPipe() }
            .transform("min") { MinPipe() }
            .transform("times") { TimesPIpe() }
            .transform("div") { DivPipe() }
            .transform("plus") { PlusPipe() }
            .transform("minus") { MinusPipe() }
            // deprecated?
            .transform("multiply") { TimesPIpe() }
            .transform("add") { PlusPipe() }

    val keys = BasicKeyResolver()
            .key(Key("Math", "PI")) { ConstKeyHandler(Math.PI) }
            .key(Key("Math", "random")) { RandomKey() }

    val conditions = BasicConditionResolver(StandardNumberConditions)

}
