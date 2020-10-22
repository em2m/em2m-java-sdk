package io.em2m.utils

import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit


object TimeAgo {

    val times = listOf(
            TimeUnit.DAYS.toMillis(365),
            TimeUnit.DAYS.toMillis(30),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.SECONDS.toMillis(1))
    val timeUnits = listOf("year", "month", "day", "hour", "minute", "second")

    fun fromNow(span: Long, withoutAffix: Boolean): String {
        var duration = span
        val prefix = when {
            (duration >= 0 && !withoutAffix) -> "in "
            else -> null
        }
        val suffix = when {
            (duration < 0 && !withoutAffix) -> " ago"
            else -> null
        }
        if (duration < 0) duration *= -1
        val res = StringBuilder()
        for (i in TimeAgo.times.indices) {
            val current = TimeAgo.times[i]
            val temp = duration / current
            if (temp > 0) {
                if (prefix != null) res.append(prefix)
                res.append(temp).append(" ").append(timeUnits[i])
                if (temp != 1L) res.append("s")
                if (suffix != null) res.append(suffix)
                break
            }
        }
        return if ("" == res.toString())
            "${prefix ?: ""}0 seconds${suffix ?: ""}"
        else
            res.toString()
    }

}


fun Duration.fromNow(withoutAffix: Boolean = false): String {
    return TimeAgo.fromNow(toMillis(), withoutAffix)
}

fun Date.nextBusinessDay(): Date {
    val c = Calendar.getInstance()
    c.time = this
    c.add(Calendar.DATE, 1)
    while ((c.get(Calendar.DAY_OF_WEEK) == 1) || (c.get(Calendar.DAY_OF_WEEK) == 7)) {
        c.add(Calendar.DATE, 1)
    }
    return c.time
}