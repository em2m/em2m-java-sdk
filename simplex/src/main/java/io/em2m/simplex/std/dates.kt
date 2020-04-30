package io.em2m.simplex.std

import io.em2m.simplex.evalPath
import io.em2m.simplex.model.*
import io.em2m.simplex.parser.DateMathParser
import io.em2m.utils.coerce
import io.em2m.utils.fromNow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class FormatDatePipe : PipeTransform {

    private val defaultZoneId = ZoneId.of("America/Los_Angeles")
    private var pattern: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-mm-dd")
            .withZone(defaultZoneId)
    private var path: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            pattern = DateTimeFormatter.ofPattern(args[0])
            if (args.size == 2) {
                if (args[1].startsWith("$")) {
                    path = args[1].removePrefix("$")
                } else {
                    try {
                        pattern = pattern.withZone(ZoneId.of(args[1]))
                    } catch (ex: Exception) {
                    }
                }
            } else {
                pattern = pattern.withZone(defaultZoneId)
            }
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val dateInput: Date? = value?.coerce()
        return if (dateInput != null) {
            val date = dateInput.toInstant()
            if (path != null) {
                val zoneId = context.evalPath(path!!)?.toString()
                val p = if (zoneId != null) {
                    try {
                        pattern.withZone(ZoneId.of(zoneId))
                    } catch( ex: Throwable) {
                        pattern
                    }
                } else pattern
                p.format(date)
            } else {
                pattern.format(date)
            }
        } else value
    }
}

class FormatDurationPipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        val longValue: Long? = value?.coerce()
        return if (longValue != null) {
            val duration = Duration.ofMillis(longValue)
            duration.fromNow(true)
        } else value
    }

}

class durationToDaysPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        val milliseconds: Long? = value?.coerce()
        return if (milliseconds != null) {
            Math.round(milliseconds!!/(1000.0*60*60*24)*10)/10.0
        } else milliseconds
    }
}

class FromNowPipe : PipeTransform {

    private var withoutAffix = false

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            withoutAffix = args[0].coerce() ?: false
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val date: Date? = value.coerce()
        return if (date != null) {
            Duration.between(Instant.now(), date.toInstant()).fromNow(withoutAffix)
        } else value
    }

}

class DateMathPipe : PipeTransform {

    private val dateMathParser = DateMathParser()
    private var dateMath: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            dateMath = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val dateInput: Date? = value?.coerce()
        return if (dateInput != null && dateMath != null) {
            dateMathParser.parse(dateMath.toString(), dateInput.time).coerce<Date>()
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
            .transform("formatDate") { FormatDatePipe() }
            .transform("formatDuration") { FormatDurationPipe() }
            .transform("dateMath") { DateMathPipe() }
            .transform("fromNow") { FromNowPipe() }
            .transform("durationToDays") { durationToDaysPipe() }

    val keys = BasicKeyResolver()
            .key(Key("Date", "now")) { _ -> DateNowHandler() }

    val conditions = BasicConditionResolver(StandardDateConditions)

}