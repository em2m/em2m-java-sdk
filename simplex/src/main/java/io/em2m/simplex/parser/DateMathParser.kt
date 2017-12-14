/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________
 *
 *
 * Copyright (c) 2013-2016 Elastic M2M Incorporated, All Rights Reserved.
 *
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated
 *
 *
 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.simplex.parser

import org.joda.time.DateTimeZone
import org.joda.time.MutableDateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.util.*

/**
 * A parser for date/time formatted text with optional date math.
 *
 *
 * The format of the datetime is configurable, and unix timestamps can also be used. Datemath
 * is appended to a datetime with the following syntax:
 * `||[+-/](\d+)?[yMwdhHms]`.
 */
class DateMathParser(private val dateTimeFormatter: FormatDateTimeFormatter = FormatDateTimeFormatter("dateOptionalTime",
        ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.UTC),
        ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC), Locale.ROOT)) {

    init {
        Objects.requireNonNull(dateTimeFormatter)
    }

    constructor(timeZone: DateTimeZone) : this(FormatDateTimeFormatter("dateOptionalTime",
            ISODateTimeFormat.dateOptionalTimeParser().withZone(timeZone),
            ISODateTimeFormat.dateTime().withZone(timeZone), Locale.ROOT))

    // Note: we take a callable here for the timestamp in order to be able to figure out
    // if it has been used. For instance, the request cache does not cache requests that make
    // use of `now`.
    @JvmOverloads
    fun parse(text: String, now: Long, roundUp: Boolean = false, timeZone: DateTimeZone? = null): Long {
        val time: Long
        val mathString: String
        if (text.startsWith("now")) {
            try {
                time = now
            } catch (e: Exception) {
                throw RuntimeException("could not read the current timestamp", e)
            }

            mathString = text.substring("now".length)
        } else {
            val index = text.indexOf("||")
            if (index == -1) {
                return parseDateTime(text, timeZone)
            }
            time = parseDateTime(text.substring(0, index), timeZone)
            mathString = text.substring(index + 2)
            if (mathString.isEmpty()) {
                return time
            }
        }

        return parseMath(mathString, time, roundUp, timeZone)
    }

    @Throws(RuntimeException::class)
    private fun parseMath(mathString: String, time: Long, roundUp: Boolean, timeZone: DateTimeZone? = DateTimeZone.UTC): Long {

        val dateTime = MutableDateTime(time, timeZone)
        var i = 0
        while (i < mathString.length) {
            val c = mathString[i++]
            val round: Boolean
            val sign: Int
            if (c == '/') {
                round = true
                sign = 1
            } else {
                round = false
                if (c == '+') {
                    sign = 1
                } else if (c == '-') {
                    sign = -1
                } else {
                    throw RuntimeException("operator not supported for date math [{}]" + mathString)
                }
            }

            if (i >= mathString.length) {
                throw RuntimeException("truncated date math [{}]" + mathString)
            }

            val num: Int
            if (!Character.isDigit(mathString[i])) {
                num = 1
            } else {
                val numFrom = i
                while (i < mathString.length && Character.isDigit(mathString[i])) {
                    i++
                }
                if (i >= mathString.length) {
                    throw RuntimeException("truncated date math [{}]" + mathString)
                }
                num = Integer.parseInt(mathString.substring(numFrom, i))
            }
            if (round) {
                if (num != 1) {
                    throw RuntimeException("rounding `/` can only be used on single unit types [{}]" + mathString)
                }
            }
            val unit = mathString[i++]
            var propertyToRound: MutableDateTime.Property? = null
            when (unit) {
                'y' -> if (round) {
                    propertyToRound = dateTime.yearOfCentury()
                } else {
                    dateTime.addYears(sign * num)
                }
                'M' -> if (round) {
                    propertyToRound = dateTime.monthOfYear()
                } else {
                    dateTime.addMonths(sign * num)
                }
                'w' -> if (round) {
                    propertyToRound = dateTime.weekOfWeekyear()
                } else {
                    dateTime.addWeeks(sign * num)
                }
                'd' -> if (round) {
                    propertyToRound = dateTime.dayOfMonth()
                } else {
                    dateTime.addDays(sign * num)
                }
                'h', 'H' -> if (round) {
                    propertyToRound = dateTime.hourOfDay()
                } else {
                    dateTime.addHours(sign * num)
                }
                'm' -> if (round) {
                    propertyToRound = dateTime.minuteOfHour()
                } else {
                    dateTime.addMinutes(sign * num)
                }
                's' -> if (round) {
                    propertyToRound = dateTime.secondOfMinute()
                } else {
                    dateTime.addSeconds(sign * num)
                }
                else -> throw RuntimeException("unit [$unit] not supported for date math [$mathString]")
            }
            if (propertyToRound != null) {
                if (roundUp) {
                    // we want to go up to the next whole value, even if we are already on a rounded value
                    propertyToRound.add(1)
                    propertyToRound.roundFloor()
                    dateTime.addMillis(-1) // subtract 1 millisecond to get the largest inclusive value
                } else {
                    propertyToRound.roundFloor()
                }
            }
        }
        return dateTime.millis
    }

    private fun parseDateTime(value: String, timeZone: DateTimeZone?): Long {
        var parser = dateTimeFormatter.parser
        if (timeZone != null) {
            parser = parser.withZone(timeZone)
        }
        try {
            return parser.parseMillis(value)
        } catch (e: IllegalArgumentException) {

            throw RuntimeException("failed to parse date field [$value] with format [${dateTimeFormatter.format}]")
        }

    }

    /**
     * A simple wrapper around [DateTimeFormatter] that retains the
     * format that was used to create it.
     */
    class FormatDateTimeFormatter(val format: String, parser: DateTimeFormatter, printer: DateTimeFormatter, locale: java.util.Locale? = null) {

        val parser: DateTimeFormatter = if (locale == null) parser.withDefaultYear(1970) else parser.withLocale(locale).withDefaultYear(1970)

        val printer: DateTimeFormatter = if (locale == null) printer.withDefaultYear(1970) else printer.withLocale(locale).withDefaultYear(1970)

    }

}