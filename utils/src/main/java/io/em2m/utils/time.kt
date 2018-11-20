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
    fun fromNow(duration: Long, withoutSuffix: Boolean): String {
        val res = StringBuffer()
        for (i in TimeAgo.times.indices) {
            val current = TimeAgo.times[i]
            val temp = duration / current
            if (temp > 0) {
                res.append(temp).append(" ").append(timeUnits.get(i)).append(if (temp != 1L) "s" else "").append(" ago")
                break
            }
        }
        return if ("" == res.toString())
            "0 seconds ago"
        else
            res.toString()
    }
}


fun Duration.fromNow(withoutSuffix: Boolean = false): String {
    return TimeAgo.fromNow(toMillis(), withoutSuffix)
}