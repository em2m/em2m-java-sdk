package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.BasicPipeTransformResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.ExprParser
import io.em2m.simplex.std.Numbers
import io.em2m.simplex.std.Strings
import org.junit.Assert
import org.junit.Test


class ExprTest : Assert() {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2")))
            .delegate(Numbers.keys)

    val simplex = Simplex().keys(keyResolver)

    @Test
    fun testParse() {
        val exprStr = "#{ns:key1 | upperCase}/#{ns:key2 | capitalize}".replace("#", "$")
        val expr = simplex.parser.parse(exprStr)
        assertNotNull(expr)
        val result = expr.call(emptyMap())
        assertNotNull(result)
        assertEquals("VALUE1/Value2", result)
    }

    @Test
    fun testContextKey() {
        val exprStr = "#{ns:key1 | upperCase}/#{ns:key2 | capitalize}".replace("#", "$")
        val expr = requireNotNull(simplex.parser.parse(exprStr))
        val keys = BasicKeyResolver(mapOf(
                Key("ns", "key1") to ConstKeyHandler("alt1"),
                Key("ns", "key2") to ConstKeyHandler("alt2")))
        val result = expr.call(mapOf("keys" to keys))
        assertEquals("ALT1/Alt2", result)
    }

    @Test
    fun testArgs() {
        val exprStr = "#{Math:PI | number:2}".replace("#", "$")
        val expr = requireNotNull(simplex.parser.parse(exprStr))
        val result = expr.call(emptyMap())
        assertEquals("3.14", result)
    }

    @Test
    fun testSelect() {
        val exprStr = "#{ns:key1 | select:labels}".replace("#", "$")
        val labels = mapOf("value1" to "Enabled", "value2" to "Disabled")
        val context = mapOf("labels" to labels)
        val result = simplex.eval(exprStr, context)
        assertEquals("Enabled", result)
    }

}