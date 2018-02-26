package io.em2m.simplex

import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.BasicPipeTransformResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.ExprParser
import io.em2m.simplex.std.Numbers
import io.em2m.simplex.std.Strings
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test


@Ignore
class PerfTest : Assert() {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "key3") to ConstKeyHandler(Math.PI)))

    val pipeResolver = BasicPipeTransformResolver()
            .delegate(Strings.pipes)
            .delegate(Numbers.pipes)

    val parser = ExprParser(keyResolver, pipeResolver)

    @Test
    fun testContextKey() {
        val exprStr = "#{ns:key1 | upperCase}/#{ns:key2 | capitalize}".replace("#", "$")
        val start = System.currentTimeMillis()
        val expr = requireNotNull(parser.parse(exprStr))
        val keys = BasicKeyResolver(mapOf(
                Key("ns", "key1") to ConstKeyHandler("alt1"),
                Key("ns", "key2") to ConstKeyHandler("alt2")))
        (0..1_000_000).forEach {
            expr.call(mapOf("keys" to keys))
        }
        val end = System.currentTimeMillis()
        val total = end - start
        val timePer = total / 1_000_000.0 * 1000.0
        println("total time: $total")
        println("Time per: $timePer microseconds")
    }

}