package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Numbers
import org.junit.Assert
import org.junit.Test


class ExprTest : Assert() {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "pie") to ConstKeyHandler(3.14),
            Key("ns", "duration") to ConstKeyHandler(210_000),
            Key("ns", "five") to ConstKeyHandler(5)))
            .delegate(Numbers.keys)


    val simplex = Simplex()
            .keys(keyResolver)

    @Test
    fun testParse() {
        val exprStr = "\${ns:key1 | upperCase}/\${ns:key2 | capitalize}"
        val expr = simplex.parser.parse(exprStr)
        assertNotNull(expr)
        val result = expr.call(emptyMap())
        assertNotNull(result)
        assertEquals("VALUE1/Value2", result)
    }

    @Test
    fun testContextKey() {
        val exprStr = "\${ns:key1 | upperCase}/\${ns:key2 | capitalize}"
        val expr = requireNotNull(simplex.parser.parse(exprStr))
        val keys = BasicKeyResolver(mapOf(
                Key("ns", "key1") to ConstKeyHandler("alt1"),
                Key("ns", "key2") to ConstKeyHandler("alt2")))
        val result = expr.call(mapOf("keys" to keys))
        assertEquals("ALT1/Alt2", result)
    }

    @Test
    fun testArgs() {
        val exprStr = "\${Math:PI | number:2}"
        val expr = requireNotNull(simplex.parser.parse(exprStr))
        val result = expr.call(emptyMap())
        assertEquals("3.14", result)
    }

    @Test
    fun testSelect() {
        val exprStr = "\${ns:key1 | select:labels}"
        val labels = mapOf("value1" to "Enabled", "value2" to "Disabled")
        val context = mapOf("labels" to labels)
        val result = simplex.eval(exprStr, context)
        assertEquals("Enabled", result)
    }


    @Test
    fun testMultiplication() {
        val exprString = "\${ns:pie | multiply:2}"
        val expected = 6.28
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val actual = expr.call(emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testMultiplicationError() {
        val exprString = "\${ns:key1 | multiply:2}"
        val expected = null
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val actual = expr.call(emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testAddition() {
        val exprString = "\${ns:five | add:2}"
        val expected = 7.0
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val actual = expr.call(emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testAdditionError() {
        val exprString = "\${ns:key1 | add:2}"
        val expected = null
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val actual = expr.call(emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testEscapeAndPrepend() {
        val exprString = "\${ns:key1 | prepend:\\:}"
        val expected = ":value1"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val actual = expr.call(emptyMap())
        assertEquals(expected, actual)
    }

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


}