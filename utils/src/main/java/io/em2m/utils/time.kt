package io.em2m.utils

import java.time.Duration
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
            else -> ""
        }
        val suffix = when {
            (duration < 0 && !withoutAffix) -> " ago"
            else -> ""
        }
        if (duration < 0 ) duration *= -1
        val res = StringBuffer()
        for (i in TimeAgo.times.indices) {
            val current = TimeAgo.times[i]
            val temp = duration / current
            if (temp > 0) {
                res.append(prefix).append(temp).append(" ").append(timeUnits[i]).append(if (temp != 1L) "s" else "").append(suffix)
                break
            }
        }
        return if ("" == res.toString())
            "${prefix}0 seconds$suffix"
        else
            res.toString()
    }

}


fun Duration.fromNow(withoutSuffix: Boolean = false): String {
    return TimeAgo.fromNow(toMillis(), withoutSuffix)
}