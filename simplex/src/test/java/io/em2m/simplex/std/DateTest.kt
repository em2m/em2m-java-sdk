package io.em2m.simplex.std

import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.utils.coerceNonNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class DateTest {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "dateKey") to ConstKeyHandler("2015-04-21T17:31:06-07"),
            Key("ns", "duration") to ConstKeyHandler(210_000)))
            .delegate(Numbers.keys)

    private val dateKeyResolver = Dates.keys

    val simplex = Simplex()
            .keys(keyResolver)
            .keys(dateKeyResolver)

    @Test
    fun testArgs() {
        val exprStr = "\${Math:PI | number:2}"
        val expr = requireNotNull(simplex.parser.parse(exprStr))
        val result = expr.call(emptyMap())
        assertEquals("3.14", result)
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
    fun testFormatDurationZero() {
        val pipe = FormatDurationPipe()
        val result = pipe.transform(0, emptyMap())
        assertEquals("0 seconds", result)
    }

    @Test
    fun testFormatDateWithColon() {
        val exprString = "\${ns:dateKey | formatDate:yyyy-MM-dd HH\\:mm:America/Los_Angeles}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertEquals("2015-04-21 17:31", result)
    }

    @Test
    fun testFormatDateWithTimeZoneReference() {
        val exprString = "\${ns:dateKey | formatDate:yyyy-MM-dd HH\\:mm:\$timeZone}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(mapOf("timeZone" to "America/Los_Angeles"))
        assertEquals("2015-04-21 17:31", result)
    }


    /*
    @Test
    fun testFormatDateZulu() {
        val exprString = "\${ns:dateKey | formatDate:yyyy-MM-dd HH\\:mm\\:ss.SSS'Z'}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertEquals("2015-04-21 17:31:06.000Z", result)
    }
     */

    @Test
    fun testFromNow() {
        val exprString = "\${ns:dateKey | fromNow}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        assertTrue(result.toString().endsWith("ago"))
    }

    @Test
    fun testDateMath() {
        val expected = "2015-05-21"
        val exprString = "\${ns:dateKey | dateMath:now+30d/d}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        val actual = SimpleDateFormat("yyyy-MM-dd").format(result)
        assertEquals(expected, actual)
    }

    @Test
    fun testDatePlus() {
        val expected = "2015-04-22"
        val exprString = "\${ns:dateKey | datePlus:1:d}"
        val expr = requireNotNull(simplex.parser.parse(exprString))
        val result = expr.call(emptyMap())
        val actual = SimpleDateFormat("yyyy-MM-dd").format(result)
        assertEquals(expected, actual)

        val exprString2 = "\${ns:dateKey | datePlus:\$offset:d}"
        val expr2 = requireNotNull(simplex.parser.parse(exprString))
        val result2 = expr.call(mapOf("offset" to 1))
        val actual2 = SimpleDateFormat("yyyy-MM-dd").format(result2)
        assertEquals(expected, actual2)

    }

}