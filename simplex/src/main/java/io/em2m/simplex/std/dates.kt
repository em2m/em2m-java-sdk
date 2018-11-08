package io.em2m.simplex.std

import io.em2m.simplex.model.*
import io.em2m.simplex.parser.DateMathParser
import io.em2m.utils.coerce
import io.em2m.utils.coerceNonNull
import org.joda.time.format.DateTimeFormat
import java.util.*

class FormatDatePipe : PipeTransform {

    private var dateFormat: String = ""

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            dateFormat = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val dateInput : Date? = value?.coerce()
         return if (dateInput != null) {
             val pattern = DateTimeFormat.forPattern(dateFormat)
             pattern.print(dateInput.time)
         } else value
    }
}

class DateMathPipe : PipeTransform {

    private val dateMathParser = DateMathParser()
    private var dateMath: String = ""

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            dateMath = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val dateInput : Date? = value?.coerce()
        return if (dateInput != null) {
            dateMathParser.parse(dateMath, dateInput.time)
        } else value
    }
}

class DateNowHandler : KeyHandler {

    override fun call(key: Key, context: ExprContext): Any? {
        return Date()
    }
}


open class SingleDateHandler(private val op: (Date?, Date?) -> Boolean) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyNumber = if (keyValue is List<*>) {
            keyValue[0]
        } else keyValue
        val valueNumber = if (conditionValue is List<*>) {
            conditionValue[0]
        } else conditionValue

        return op(keyNumber.coerce(), valueNumber.coerce())
    }
}

fun compareDates(n1: Date?, n2: Date?): Int {
    return when {
        (n1?.time == n2?.time) -> 0
        (n2?.time == null) -> 1
        (n1?.time == null) -> -1
        else -> n1.compareTo(n2)
    }
}

class DateEquals : SingleDateHandler({ k, v -> k == v })
class DateNotEquals : SingleDateHandler({ k, v -> compareDates(k, v) != 0 })
class DateLessThan : SingleDateHandler({ k, v -> compareDates(k, v) < 0 })
class DateLessThanEquals : SingleDateHandler({ k, v -> compareDates(k, v) <= 0 })
class DateGreaterThan : SingleDateHandler({ k, v -> compareDates(k, v) > 0 })
class DateGreaterThanEquals : SingleDateHandler({ k, v -> compareDates(k, v) >= 0 })

val StandardDateConditions = mapOf(
        "DateEquals" to DateEquals(),
        "DateNotEquals" to DateNotEquals(),
        "DateLessThan" to DateLessThan(),
        "DateLessThanEquals" to DateLessThanEquals(),
        "DateGreaterThan" to DateGreaterThan(),
        "DateGreaterThanEquals" to DateGreaterThanEquals()
)


object Dates {

    val pipes = BasicPipeTransformResolver()
            .transform("formatDate") { _ -> FormatDatePipe() }
            .transform("dateMath") { _ -> DateMathPipe() }

    val keys = BasicKeyResolver()
            .key(Key("Date", "now")) { _ -> DateNowHandler() }

    val conditions = BasicConditionResolver(StandardDateConditions)

}