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

class RandomKey : KeyHandler {

    override fun call(key: Key, context: ExprContext): Any? {
        return Math.random()
    }

}

object Numbers {

    val pipes = BasicPipeTransformResolver()
            .transform("number", { _ -> NumberPipe() })
            .transform("round", { _ -> RoundPipe() })

    val keys = BasicKeyResolver()
            .key(Key("Math", "PI"), { _ -> ConstKeyHandler(Math.PI) })
            .key(Key("Math", "random"), { _ -> RandomKey() })

}