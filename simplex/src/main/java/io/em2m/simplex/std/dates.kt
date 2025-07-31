package io.em2m.simplex.std

import io.em2m.simplex.evalPath
import io.em2m.simplex.model.*
import io.em2m.simplex.parser.DateMathParser
import io.em2m.utils.coerce
import io.em2m.utils.fromNow
import io.em2m.utils.nextBusinessDay
import org.joda.time.DateTimeZone
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

private  val USformatter = DateTimeFormatter.ofPattern(
    ""
        + "[yyyy-MM-dd'T'HH:mm-ss]"
        + "[yyyy-MM-dd'T'HH:mm:ssZ]"
        + "[yyMMddHHmmssZ]"
        + "[yyyy-MM-dd]"
        + "[dd-MM-yyyy]"
        + "[yyyyMMdd]"
        + "[dd/MM/yyyy]"
        + "[yyyy/MM/dd]"
)

private val EuropeanFormatter = DateTimeFormatter.ofPattern(
    ""
        + "[yyyy-MM-dd'T'HH:mm:ssZ]"
        + "[yyddMMHHmmssZ]"
        + "[yyyy-dd-MM]"
        + "[MM-dd-yyyy]"
        + "[yyyyddMM]"
        + "[MM/dd/yyyy]"
        + "[yyyy/dd/MM]"
)

fun Any?.toDate(zone:ZoneId? = ZoneId.of("America/Los_Angeles")): Date? {
    return when (this) {
        is Date -> {
            this
        }
        is Number -> {
            Date(this.toLong())
        }
        is String ->{
            try{
                Date.from(LocalDate.parse(this, USformatter).atStartOfDay(zone).toInstant())
            }catch (ex: DateTimeParseException){
                try{
                    Date.from(LocalDate.parse(this, EuropeanFormatter).atStartOfDay(zone).toInstant())
                }catch (ex: Exception){
                    this.coerce()
                }
            }
        }
        else -> this.coerce()
    }
}

class ParseDatePipe : PipeTransform {
    private var zoneId = ZoneId.of("America/Los_Angeles")
    var pattern = "YYYY-mm-dd"
    var returnMliseconds = false
    private var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-mm-dd")
        .withZone(zoneId)
    private var path: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            pattern = args[0]
            if( pattern.isEmpty()){
                returnMliseconds = true
                if(args.size == 2  && args[1].isNotEmpty()){
                    zoneId = ZoneId.of(args[1])
                }
                return
            }
            formatter = DateTimeFormatter.ofPattern(pattern)
            if (args.size == 2) {
                if (args[1].startsWith("$")) {
                    path = args[1].removePrefix("$").trim()
                } else {
                    try {
                        zoneId = ZoneId.of(args[1])
                        formatter = formatter.withZone(zoneId)
                    } catch (ex: Exception) {
                    }
                }
            } else {
                formatter = formatter.withZone(zoneId)
            }
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value != null) {
            try {
                if (returnMliseconds) {
                    value.toDate(zoneId)?.time
                } else{
                    val sdf = SimpleDateFormat(pattern)

                    sdf.parse(value.toString())

                }
                //val epoch = date.atStartOfDay(zoneId).toEpochSecond() * 1000
                //Date(epoch)
            } catch (ex: java.lang.Exception) {
                null
            }
        } else null


    }

}

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
                    path = args[1].removePrefix("$").trim()
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
        val dateInput: Date? = value.toDate()
        return if (dateInput != null) {
            val date = dateInput.toInstant()
            if (path != null) {
                val zoneId = context.evalPath(path!!)?.toString()
                val p = if (zoneId != null) {
                    try {
                        pattern.withZone(ZoneId.of(zoneId))
                    } catch (ex: Throwable) {
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

    private var units: String? = null

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            units = args[0].coerce()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val longValue: Long? = value?.coerce()
        return if (longValue != null) {
            val duration = Duration.ofMillis(longValue)
            when (units?.trim()) {
                "days" -> duration.toDays()
                "hours" -> duration.toHours()
                "minutes" -> duration.toMinutes()
                "seconds" -> duration.toMillis() / 1000
                "HHhMMmSSs" -> {
                    val hours = duration.toHours()
                    val minutes = (duration.toMinutes() % 60)
                    val seconds = (duration.seconds % 60)
                    "${hours}H ${minutes}M ${seconds}S"
                }
                else -> duration.fromNow(true)
            }
        } else value
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
        val date: Date? = value.toDate()
        return if (date != null) {
            Duration.between(Instant.now(), date.toInstant()).fromNow(withoutAffix)
        } else value
    }

}

class FromNowUnitsPipe : PipeTransform {

    private var units = "d"

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            units = args[0].coerce() ?: "d"
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val date: Date? = value.toDate()
        return if (date != null) {
            val duration = Duration.between(date.toInstant(), Instant.now())
            when (units) {
                "d" -> duration.toDays()
                "h" -> duration.toHours()
                "m" -> duration.toMinutes()
                "s" -> duration.toMillis() / 1000
                else -> duration.toDays()
            }
        } else value
    }

}

class DateMathPipe : PipeTransform {

    private val dateMathParser = DateMathParser()
    private var dateMath: String? = null
    private var zonePath: String? = null
    private var zone: DateTimeZone = DateTimeZone.forID("America/Los_Angeles")

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            dateMath = args.first().trim()

            if (args.size > 1) {
                if (args[1].startsWith("$")) {
                    zonePath = args[1].removePrefix("$").trim()
                } else {
                    try {
                        zone = DateTimeZone.forID(args[1])
                    } catch (ex: Exception) {
                    }
                }
            }
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val dateInput: Date? = value.toDate()
        return if (dateInput != null && dateMath != null) {
            var timeZone: DateTimeZone = zone
            if (zonePath != null) {
                val zoneId = context.evalPath(zonePath!!)?.toString()
                try {
                    timeZone = DateTimeZone.forID(zoneId)
                } catch (ex: Exception) {
                }
            }
            dateMathParser.parse(dateMath.toString(), dateInput.time, false, timeZone).coerce<Date>()
        } else value
    }
}

class NextBusinessDayPipe : PipeTransform {

    private var zonePath: String? = null
    private var zone: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            if (args[0].startsWith("$")) {
                zonePath = args[0].removePrefix("$").trim()
            } else {
                try {
                    zone = TimeZone.getTimeZone(args[0])
                } catch (ex: Exception) {
                }
            }
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val dateInput: Date? = value.toDate()
        return if (dateInput != null) {
            var timeZone: TimeZone = zone
            if (zonePath != null) {
                val zoneId = context.evalPath(zonePath!!)?.toString()
                try {
                    timeZone = TimeZone.getTimeZone(zoneId)
                } catch (ex: Exception) {
                }
            }

            dateInput.nextBusinessDay(timeZone)
        } else value
    }
}

class WeekEndingPipe : PipeTransform {

    private var zonePath: String? = null
    private var zone: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")
    private var pattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM")
        .withZone(zone.toZoneId())

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            if (args[0].startsWith("$")) {
                zonePath = args[0].removePrefix("$").trim()
            } else {
                try {
                    zone = TimeZone.getTimeZone(args[0])
                } catch (ex: Exception) {
                }
            }
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val dateInput: Date? = value.toDate()
        return if (dateInput != null) {
            var timeZone: TimeZone = zone
            if (zonePath != null) {
                val zoneId = context.evalPath(zonePath!!)?.toString()
                try {
                    timeZone = TimeZone.getTimeZone(zoneId)
                } catch (ex: Exception) {
                }
            }

            var weekEnd = dateInput.toInstant().atZone(timeZone.toZoneId()).toLocalDate()
            while (weekEnd.dayOfWeek != DayOfWeek.SATURDAY) {
                weekEnd = weekEnd.plusDays(1);
            }
            pattern.format(weekEnd)
        } else null
    }
}

class DatePlusPipe : PipeTransform {

    var units: String = "d"
    var amount = 1
    var path: String? = null
    var zonePath: String? = null
    var zone: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0.startsWith("$")) {
                path = arg0.removePrefix("$").trim()
            } else {
                amount = arg0.toInt()
            }
        }
        if (args.size > 1) {
            units = args[1].trim()
        }
        if (args.size > 2) {
            if (args[2].startsWith("$")) {
                zonePath = args[2].removePrefix("$").trim()
            } else {
                try {
                    zone = TimeZone.getTimeZone(args[2])
                } catch (ex: Exception) {
                }
            }
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {

        val dateInput: Date? = value.toDate()
        return if (dateInput != null) {
            val p = path
            val amount: Int = if (p != null) {
                context["variables"].evalPath(p).coerce() ?: context["fieldValues"].evalPath(p).coerce()
                ?: context.evalPath(p).coerce() ?: 0
            } else {
                this.amount
            }

            var timeZone: TimeZone = zone
            if (zonePath != null) {
                val zoneId = context.evalPath(zonePath!!)?.toString()
                try {
                    timeZone = TimeZone.getTimeZone(zoneId)
                } catch (ex: Exception) {
                }
            }

            val c = Calendar.getInstance()
            c.timeZone = timeZone
            c.time = dateInput
            when (units) {
                "s" -> c.add(Calendar.SECOND, amount)
                "m" -> c.add(Calendar.MINUTE, amount)
                "h" -> c.add(Calendar.HOUR, amount)
                "d" -> c.add(Calendar.DATE, amount)
                "w" -> c.add(Calendar.WEEK_OF_MONTH, amount)
                "M" -> c.add(Calendar.MONTH, amount)
                "y" -> c.add(Calendar.YEAR, amount)
                else -> {
                }
            }
            c.time
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

        return op(keyNumber.toDate(), valueNumber.toDate())
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
        .transform("parseDate") { ParseDatePipe() }
        .transform("formatDuration") { FormatDurationPipe() }
        .transform("dateMath") { DateMathPipe() }
        .transform("fromNow") { FromNowPipe() }
        .transform("fromNowUnits") { FromNowUnitsPipe() }
        .transform("nextWeekDay", NextBusinessDayPipe())
        .transform("datePlus") { DatePlusPipe() }
        .transform("weekEnding") { WeekEndingPipe() }

    val keys = BasicKeyResolver()
        .key(Key("Date", "now")) { DateNowHandler() }

    val conditions = BasicConditionResolver(StandardDateConditions)

}
