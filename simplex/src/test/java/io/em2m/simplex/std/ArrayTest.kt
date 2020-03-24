package io.em2m.simplex.std

import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import org.junit.Test
import kotlin.test.assertEquals


class ArrayTest {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler(listOf("A", "B", "C")),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "pie") to ConstKeyHandler(3.14),
            Key("ns", "duration") to ConstKeyHandler(210_000),
            Key("ns", "five") to ConstKeyHandler(5)))
            .delegate(Numbers.keys)


    val simplex = Simplex().keys(keyResolver)

    @Test
    fun `Size of the array using a pipe`() {
        val result = simplex.eval("\${ns:key1 | size}", emptyMap())
        assertEquals(3, result)
    }

    @Test
    fun `First item in the array`() {
        val result = simplex.eval("\${ns:key1 | first}", emptyMap())
        assertEquals("A", result)
    }

    @Test
    fun `Last item in the array`() {
        val result = simplex.eval("\${ns:key1 | last}", emptyMap())
        assertEquals("C", result)
    }

    @Test
    fun `Reversed items in the array`() {
        val result = simplex.eval("\${ns:key1 | reversed | first}", emptyMap())
        assertEquals("C", result)
    }

}