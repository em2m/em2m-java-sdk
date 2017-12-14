package io.em2m.simplex.pipes

import io.em2m.simplex.model.PipeTransform
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

