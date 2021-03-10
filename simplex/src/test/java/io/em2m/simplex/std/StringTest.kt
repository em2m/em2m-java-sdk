package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Numbers
import org.junit.Assert
import org.junit.Test


class StringTest : Assert() {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "key3") to ConstKeyHandler("value1, value2"),
            Key("ns", "key4") to ConstKeyHandler(listOf("value1, value2", "value3", "value4, value5")),
            Key("ns", "key5") to ConstKeyHandler("a:b"),
            Key("ns", "key6") to ConstKeyHandler("a/b"),
            Key("ns", "key7") to ConstKeyHandler("3112114111"),
            Key("ns", "key8") to ConstKeyHandler("31121141112"),
            Key("ns", "key9") to ConstKeyHandler(null),
            Key("ns", "key10") to ConstKeyHandler("(123) 456-7890"),
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
    fun testEscapeAndPrepend() {
        val exprString = "\${ns:key1 | prepend:\\:}"
        val expected = ":value1"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val actual = expr.call(emptyMap())
        assertEquals(expected, actual)
    }

    @Test
    fun testSplit() {
        val exprString1 = "\${ns:key3 | split:, }"
        val expected1 = listOf("value1", "value2")
        val expr1 = requireNotNull(simplex.parser.parse(exprString1))
        val actual1 = expr1.call(emptyMap())

        val exprString2 = "\${ns:key4 | split:, }"
        val expected2 = listOf("value1", "value2", "value3", "value4", "value5")
        val expr2 = requireNotNull(simplex.parser.parse(exprString2))
        val actual2 = expr2.call(emptyMap())

        val exprString3 = "\${ns:key5 | split:\\:}"
        val expected3 = listOf("a", "b")
        val expr3 = requireNotNull(simplex.parser.parse(exprString3))
        val actual3 = expr3.call(emptyMap())

        val exprString4 = "\${ns:key6 | split:/}"
        val expected4 = listOf("a", "b")
        val expr4 = requireNotNull(simplex.parser.parse(exprString4))
        val actual4 = expr4.call(emptyMap())

        assertEquals(expected1, actual1)
        assertEquals(expected2, actual2)
        assertEquals(expected3, actual3)
        assertEquals(expected4, actual4)
    }

    @Test
    fun testUrlEncode() {
        val encodeExpr = "\${ns:key5 | urlEncode}"
        val roundTripExpr = "\${ns:key5 | urlEncode | urlDecode}"
        val expected = "a%3Ab"
        assertEquals(expected, simplex.eval(encodeExpr, emptyMap()))
        assertEquals("a:b", simplex.eval(roundTripExpr, emptyMap()))
    }

    @Test
    fun testFormatPhonePipe() {
        val exprString1 = "\${ns:key7 | formatPhone}"
        val expected1 = "(311) 211-4111"
        val expr1 = requireNotNull(simplex.parser.parse(exprString1))
        val actual1 = expr1.call(emptyMap())

        val exprString2 = "\${ns:key8 | formatPhone}"
        val expected2 = "31121141112"
        val expr2 = requireNotNull(simplex.parser.parse(exprString2))
        val actual2 = expr2.call(emptyMap())

        val exprString3 = "\${ns:key9 | formatPhone}"
        val expected3 = null
        val expr3 = requireNotNull(simplex.parser.parse(exprString3))
        val actual3 = expr3.call(emptyMap())

        assertEquals(expected1, actual1)
        assertEquals(expected2, actual2)
        assertEquals(expected3, actual3)
    }

    @Test
    fun RemoveCharsPipe() {
        val exprString1 = "\${ns:key10 | removeChars:() -}"
        val expected1 = "1234567890"
        val expr1 = requireNotNull(simplex.parser.parse(exprString1))
        val actual1 = expr1.call(emptyMap())

        val exprString2 = "\${ns:key10 | removeChars:}"
        val expected2 = "(123) 456-7890"
        val expr2 = requireNotNull(simplex.parser.parse(exprString2))
        val actual2 = expr2.call(emptyMap())

        assertEquals(expected1, actual1)
        assertEquals(expected2, actual2)
    }
}
