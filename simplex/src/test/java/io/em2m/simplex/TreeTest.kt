package io.em2m.simplex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Expr
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.SimplexModule
import io.em2m.simplex.std.Numbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test


class TreeTest {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2")))
            .delegate(Numbers.keys)

    val simplex = Simplex().keys(keyResolver)

    val mapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))

    @Test
    fun testParseValue() {
        val expr: Expr = mapper.readValue(""" "#{ns:key1 | upperCase}" """.replace("#", "$"))
        val value = expr.call(emptyMap())
        requireNotNull(expr)
        assertEquals("VALUE1", value)
    }

    @Test
    fun testParseObject() {
        val json = """ {
            "obj": {
                "v1": "#{ns:key1 | capitalize}",
                "v2": "#{ns:key2 | upperCase}"
            },
            "value": "#{ns:key1} #{ns:key2}"
        }
        """.trimIndent().replace("#", "$")

        val rule: Rule = mapper.readValue(json)
        assertNotNull(rule)
        val obj = rule.obj.call(emptyMap()) as Map<String, Any?>
        assertNotNull(obj)
        assertEquals("Value1", obj["v1"])
        assertEquals("VALUE2", obj["v2"])
        val value = rule.value.call(emptyMap())
        assertEquals("value1 value2", value)
    }

    data class Rule(val obj: Expr, val value: Expr)

}