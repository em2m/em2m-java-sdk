package io.em2m.simplex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.*
import io.em2m.simplex.parser.SimplexModule
import io.em2m.simplex.std.Dates
import io.em2m.simplex.std.Numbers
import kotlin.test.Test
import kotlin.test.assertNotNull

class TreeExprTest {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "dateKey") to ConstKeyHandler("2015-04-21T17:31:06-07")))
            .delegate(Numbers.keys)

    val dateKeyResolver = Dates.keys

    val simplex = Simplex()
            .keys(keyResolver)
            .keys(dateKeyResolver)

    val objectMapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))

    @Test
    fun testTree() {
        val expr: Expr = objectMapper.readValue(jsonExpr)
        val context = HashMap<String, Any?>()
        val data = expr.call(context)
        assertNotNull(data)
    }

    val jsonExpr = """
        {
           "key1": "#{ns:key1 | upperCase}",
           "key2": "#{ns:key2 | upperCase}"
        }
    """.trimIndent().replace("#", "$")
}