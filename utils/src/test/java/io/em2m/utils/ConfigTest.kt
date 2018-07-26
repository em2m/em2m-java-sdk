package io.em2m.utils

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.junit.Test
import kotlin.test.assertEquals


class ConfigTest {

    private val config: Config = ConfigFactory.load("test")

    @Test
    fun testCoerce() {
        val v1: String? = config.safeGet("a.b.c.string").coerce()
        val v2: String? = config.safeGet("a.b.c.missing").coerce("value2")
        assertEquals("value", v1)
        assertEquals("value2", v2)

        assertEquals(42, config.safeGet("a.b.c.int"))
        assertEquals(3.1415, config.safeGet("a.b.c.number").coerce()!!)
        assertEquals(3.1415F, config.safeGet("a.b.c.number").coerce()!!)
        assertEquals(3, config.safeGet("a.b.c.number").coerce()!!)
        assertEquals("3.1415", config.safeGet("a.b.c.number").coerce()!!)
    }

}