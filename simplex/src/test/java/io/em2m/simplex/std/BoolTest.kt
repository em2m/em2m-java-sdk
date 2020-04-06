package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Numbers
import org.junit.Assert
import org.junit.Test


class BoolTest : Assert() {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler(true),
            Key("ns", "key2") to ConstKeyHandler(false),
            Key("ns", "key3") to ConstKeyHandler(null)))
            .delegate(Numbers.keys)


    val simplex = Simplex()
            .keys(keyResolver)

    @Test
    fun testParse() {
        val trueExprStr = "\${ns:key1 | boolLabel:TrueLabel:FalseLabel:NullLabel}"
        val trueExpr = simplex.parser.parse(trueExprStr)
        assertEquals("TrueLabel", trueExpr.call(emptyMap()))

        val falseExprStr = "\${ns:key2 | boolLabel:TrueLabel:FalseLabel:NullLabel}"
        val falseExpr = simplex.parser.parse(falseExprStr)
        assertEquals("FalseLabel", falseExpr.call(emptyMap()))

        val nullExprStr = "\${ns:key3 | boolLabel:TrueLabel:FalseLabel:NullLabel}"
        val nullExpr = simplex.parser.parse(nullExprStr)
        assertEquals("NullLabel", nullExpr.call(emptyMap()))

        val emptyExprStr = "\${ns:key3 | boolLabel:TrueLabel:FalseLabel}"
        val emptyStr = simplex.parser.parse(emptyExprStr)
        assertEquals("", emptyStr.call(emptyMap()))
    }
}