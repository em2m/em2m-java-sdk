package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Dates
import io.em2m.simplex.std.Numbers
import io.em2m.utils.coerce
import io.em2m.utils.coerceNonNull
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.junit.Assert
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*


class ExprTest : Assert() {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "dateKey") to ConstKeyHandler("2015-04-21")))
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
        assertTrue(Date().time - 1000 < resultingDate.time && resultingDate.time < Date().time + 1000 )
    }

    @Test
    fun testFormatDate() {
        val exprString = "\${ns:dateKey | formatDate:yyyy}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertEquals(result, "2015")
    }

    @Test
    fun testDateMath() {
        var pattern: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
        val actual = pattern.print(DateTime(("2015-04-21").coerceNonNull<Date>()).plusDays(30))
        val exprString = "\${ns:dateKey | dateMath:now+30d/d}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertEquals(SimpleDateFormat("yyyy-MM-dd").format(result), actual)
    }

}