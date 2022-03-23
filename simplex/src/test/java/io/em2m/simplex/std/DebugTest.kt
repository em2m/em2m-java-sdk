package io.em2m.simplex.std

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Expr
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.SimplexModule
import org.junit.Test
import kotlin.test.assertEquals


class DebugTest {

    private val test = jacksonObjectMapper().createArrayNode().add("A").add("B").add("C")

    private val keyResolver = BasicKeyResolver(
        mapOf(
            Key("ns", "key1") to ConstKeyHandler(listOf("A", "B", "C")),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "pie") to ConstKeyHandler(3.14),
            Key("ns", "duration") to ConstKeyHandler(210_000),
            Key("ns", "five") to ConstKeyHandler(5),
            Key("ns", "arrayNode") to ConstKeyHandler(test)
        )
    )
        .delegate(Numbers.keys)

    val simplex = Simplex().keys(keyResolver)
    private val mapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))


    @Test
    fun `Log using a pipe`() {
        simplex.eval("\${ns:key1 | log | first | log}", emptyMap())
    }

    @Test
    fun `Log using an exec`() {
        val expr: Expr = mapper.readValue( """
                    {
                     "@exec": "log",
                     "level": "debug",
                     "value": "#{ns:key1}"
                    }
                    """.replace("#", "$"))
        val result = expr.call(emptyMap())
    }

}
