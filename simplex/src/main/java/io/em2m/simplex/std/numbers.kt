package io.em2m.simplex.std

import io.em2m.simplex.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class NumberPipe : PipeTransform {

    private val format: DecimalFormat = DecimalFormat().apply { maximumFractionDigits = 3 }

    override fun args(args: List<String>) {
        if (args.size > 0) {
            val fractionSize = args[0].toInt()
            format.maximumFractionDigits = fractionSize
        }
    }

    override fun transform(value: Any?): Any? {
        return if (value != null) {
            if (value is Number) {
                format.format(value)
            } else ""
        } else null
    }

}

class PrecisionPipe : PipeTransform {

    private var precision: Int = 3

    override fun args(args: List<String>) {
        if (args.size > 0) {
            precision = Integer.parseInt(args[0])
        }
    }

    override fun transform(value: Any?): Any? {
        return if (value is Number) {
            BigDecimal(value.toDouble()).setScale(precision, RoundingMode.HALF_UP)
        } else null
    }

}

class RandomKey : KeyHandler {

    override fun call(key: Key, context: ExprContext): Any? {
        return Math.random()
    }

}

object Numbers {

    val pipes = BasicPipeTransformResolver()
            .transform("number", { _ -> NumberPipe() })
            .transform("precision", { _ -> PrecisionPipe() })

    val keys = BasicKeyResolver()
            .key(Key("Math", "PI"), { _ -> ConstKeyHandler(Math.PI) })
            .key(Key("Math", "random"), { _ -> RandomKey() })

}