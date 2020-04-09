package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Numbers
import org.junit.Assert
import org.junit.Test


class ByteTest : Assert() {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "pie") to ConstKeyHandler(3.14),
            Key("ns", "duration") to ConstKeyHandler(210_000),
            Key("ns", "21") to ConstKeyHandler(21),
            Key("ns", "zero") to ConstKeyHandler(0),
            Key("ns", "five") to ConstKeyHandler(5)))
            .delegate(Numbers.keys)


    val simplex = Simplex()
            .keys(keyResolver)


    @Test
    fun testBase64() {
        val exprString = "\${ns:key1 | encodeBase64}"
        val expected = "dmFsdWUx"
        val actual = simplex.eval(exprString, emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testBase64_2() {
        val exprString = "\${ns:key1 | encodeBase64 | decodeBase64}"
        val expected = "value1"
        val actual = String(simplex.eval(exprString, emptyMap()) as ByteArray)
        assertEquals(expected, actual)
    }

    @Test
    fun testHexEncode() {
        val exprString = "\${ns:key1 | encodeHex}"
        val expected = "76616c756531"
        val actual = simplex.eval(exprString, emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testHexEncode21() {
        val exprString = "\${ns:21 | encodeHex}"
        val expected = "15"
        val actual = simplex.eval(exprString, emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testHexEncode5() {
        val exprString = "\${ns:five | encodeHex}"
        val expected = "05"
        val actual = simplex.eval(exprString, emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testHexEncodeZero() {
        val exprString = "\${ns:zero | encodeHex}"
        val expected = "00"
        val actual = simplex.eval(exprString, emptyMap())
        assertEquals(expected, actual)
    }
}