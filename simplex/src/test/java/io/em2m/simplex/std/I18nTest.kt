package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Numbers
import org.junit.Assert
import org.junit.Test


class I18nTest : Assert() {

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
    fun testSelect() {
        val exprStr = "\${ns:key1 | select:labels}"
        val labels = mapOf("value1" to "Enabled", "value2" to "Disabled")
        val context = mapOf("labels" to labels)
        val result = simplex.eval(exprStr, context)
        assertEquals("Enabled", result)
    }
}