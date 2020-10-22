package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.BasicPipeTransformResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.ExprParser
import io.em2m.simplex.std.Numbers
import io.em2m.simplex.std.Strings
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertNotNull

class ValueTest {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "key3") to ConstKeyHandler(Math.PI)))

    private val pipeResolver = BasicPipeTransformResolver()
            .delegate(Strings.pipes)
            .delegate(Numbers.pipes)

    private val parser = ExprParser(keyResolver, pipeResolver)

    @Test
    fun testKeyWithoutHandler() {
        val exprStr = "\${f:test}"
        val expr = parser.parse(exprStr)
        assertNotNull(expr)
    }

}