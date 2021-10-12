package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Numbers
import org.junit.Assert
import org.junit.Test


class NumberTest : Assert() {

    private val keyResolver = BasicKeyResolver(
        mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "pie") to ConstKeyHandler(3.14),
            Key("ns", "duration") to ConstKeyHandler(210_000),
            Key("ns", "five") to ConstKeyHandler(5),
            Key("ns", "long") to ConstKeyHandler(15115006704646)
        )
    ).delegate(Numbers.keys)


    val simplex = Simplex()
        .keys(keyResolver)

    @Test
    fun testArgs() {
        val exprStr = "\${Math:PI | number:2}"
        val expr = requireNotNull(simplex.parser.parse(exprStr))
        val result = expr.call(emptyMap())
        assertEquals("3.14", result)
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
    fun testNumberPiper() {
        val expected = "15,115,006,704,646"
        val exprString = "\${ns:long | number}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val actual = expr.call(emptyMap())
        assertEquals(expected, actual)
    }
}
