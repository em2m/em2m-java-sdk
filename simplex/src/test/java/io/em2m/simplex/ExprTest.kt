package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Dates
import io.em2m.simplex.std.Numbers
import io.em2m.utils.coerceNonNull
import org.junit.Assert
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class ExprTest : Assert() {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "dateKey") to ConstKeyHandler("2015-04-21T17:31:06-07"),
            Key("ns", "pie") to ConstKeyHandler(3.14),
            Key("ns", "duration") to ConstKeyHandler(210_000),
            Key("ns", "five") to ConstKeyHandler(5)))
            .delegate(Numbers.keys)

    val dateKeyResolver = Dates.keys

    val simplex = Simplex()
            .keys(keyResolver)
            .keys(dateKeyResolver)

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
    fun testDateNow() {
        val exprString = "\${Date:now}"
        val expr = simplex.parser.parse(exprString)
        val resultingDate: Date = expr.call(emptyMap()).coerceNonNull()
        assertTrue(Date().time - 1000 < resultingDate.time && resultingDate.time < Date().time + 1000)
    }

    @Test
    fun testFormatDate() {
        val exprString = "\${ns:dateKey | formatDate:yyyy}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertEquals("2015", result)
    }

    @Test
    fun testFormatDuration() {
        val exprString = "\${ns:duration | formatDuration}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertEquals("3 minutes", result)
    }

    @Test
    fun testFormatDateWithColon() {
        val exprString = "\${ns:dateKey | formatDate:yyyy-MM-dd HH\\:mm:America/Los_Angeles}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertEquals("2015-04-21 17:31", result)
    }

    @Test
    fun testFromNow() {
        val exprString = "\${ns:dateKey | fromNow}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertTrue(result.toString().endsWith("ago"))
    }

    @Test
    fun testDateMath() {
        val pattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("America/New_York"))
        val expected = "2015-05-21"
        val exprString = "\${ns:dateKey | dateMath:now+30d/d}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        val actual = SimpleDateFormat("yyyy-MM-dd").format(result)
        assertEquals(expected, actual)
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

}