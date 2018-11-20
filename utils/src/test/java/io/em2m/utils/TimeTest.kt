package io.em2m.utils

import org.junit.Test
import java.time.Duration

class TimeTest {

    @Test
    fun testFromNow() {
        val fromNow = Duration.ofDays(5).fromNow()
        println(fromNow)
    }
}