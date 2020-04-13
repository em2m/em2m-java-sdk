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

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "key3") to ConstKeyHandler(Math.PI)))

    private val pipeResolver = BasicPipeTransformResolver()
            .delegate(Strings.pipes)
            .delegate(Numbers.pipes)

    private val parser = ExprParser(keyResolver, pipeResolver)

    @Test
    fun testContextKey() {
        val exprStr = "\${ns:key1 | upperCase}/\${ns:key2 | capitalize}"
        val start = System.currentTimeMillis()
        val expr = requireNotNull(parser.parse(exprStr))
        val keys = BasicKeyResolver(mapOf(
                Key("ns", "key1") to ConstKeyHandler("alt1"),
                Key("ns", "key2") to ConstKeyHandler("alt2")))
        repeat(1_000_000) {
            expr.call(mapOf("keys" to keys))
        }
        val end = System.currentTimeMillis()
        val total = end - start
        val timePer = total / 1_000_000.0 * 1000.0
        println("total time: $total")
        println("Time per: $timePer microseconds")
    }

    @Test
    fun testSinglePipe() {
        val exprStr = "\${ns:key1}"
        val start = System.currentTimeMillis()
        val expr = requireNotNull(parser.parse(exprStr))
        val keys = BasicKeyResolver(mapOf(
                Key("ns", "key1") to ConstKeyHandler("alt1"),
                Key("ns", "key2") to ConstKeyHandler("alt2")))
        val ctx = mapOf("keys" to keys)
        repeat(1_000_000) {
            expr.call(ctx)
        }
        val end = System.currentTimeMillis()
        val total = end - start
        val timePer = total / 1_000_000.0 * 1000.0
        println("total time: $total")
        println("Time per: $timePer microseconds")
    }

    @Test
    fun testSinglePipeUsingHelper() {
        val simplex = Simplex()
        val exprStr = "\${ns:key1}"
        val start = System.currentTimeMillis()
        //val expr = requireNotNull(parser.parse(exprStr))
        val keys = BasicKeyResolver(mapOf(
                Key("ns", "key1") to ConstKeyHandler("alt1"),
                Key("ns", "key2") to ConstKeyHandler("alt2")))
        val ctx = mapOf("keys" to keys)
        repeat(1_000_000) {
            simplex.eval(exprStr, ctx)
        }
        val end = System.currentTimeMillis()
        val total = end - start
        val timePer = total / 1_000_000.0 * 1000.0
        println("total time: $total")
        println("Time per: $timePer microseconds")
    }

    @Test
    fun testParser() {
        val exprStr = "\${ns:key1 | upperCase}/\${ns:key2 | capitalize}"
        val start = System.currentTimeMillis()
        repeat(1_000_000) {
            assertNotNull(parser.parse(exprStr))
        }
        val end = System.currentTimeMillis()
        val total = end - start
        val timePer = total / 1_000_000.0 * 1000.0
        println("total time: $total")
        println("Time per: $timePer microseconds")
    }

}