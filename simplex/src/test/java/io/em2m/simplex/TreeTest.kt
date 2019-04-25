package io.em2m.simplex

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.*
import io.em2m.simplex.parser.SimplexModule
import io.em2m.simplex.std.Numbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test


class TreeTest {

    val people = listOf(mapOf("name" to "Fred"), mapOf("name" to "Barney"), mapOf("name" to "Wilma"), mapOf("name" to "Betty"))

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("ns", "people") to ConstKeyHandler(people)
    ))
            .delegate(Numbers.keys)

    val simplex = Simplex().keys(keyResolver)

    val mapper = jacksonObjectMapper().registerModule(SimplexModule(simplex)).enable(SerializationFeature.INDENT_OUTPUT)

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

    @Test
    fun testTags() {
        val json = """
        {
          "Hello": {
            "@if": "#{ns:key1 | cond:StringEquals:value1}",
            "@repeat": "#{ns:people}",
            "name": "#{repeat:item.name}",
            "index": "#{repeat:index}",
            "@container": [
                {
                  "@if": "#{repeat:index | plus:1 | cond:NumberEquals:2}",
                  "second": "This is the second item"
                },
                {
                  "@if": "#{repeat:odd}",
                  "label": "This item is odd!"
                },
                {
                  "@if": "#{repeat:even}",
                  "label": "This item is even!"
                },
                {
                  "@if": "#{repeat:last}",
                  "last": "This is the last item!"
                },
                {
                  "@if": "#{repeat:first}",
                  "first": "This is the first item!"
                },
                {
                  "index": "#{repeat:index}"
                }
            ]
          },
          "Goodbye": {
            "@if": {
              "StringEquals": {
                "ns:key1" : "value1"
              }
            },
            "@value": "Space"
          },
         "PS": {
            "@when": [
                {
                  "@if": "#{repeat:first}",
                  "label": "This is the first item!"
                },
                {
                  "@if": "#{repeat:index | plus:1 | cond:NumberEquals:2}",
                  "label": "This is the second item"
                },
                {
                  "@if": "#{repeat:last}",
                  "label": "This is the last item!"
                },
                {
                  "@if": "#{repeat:odd}",
                  "label": "This item is odd and not first, second or last!!"
                },
                {
                  "label": "This item is even and not first, second, or last!"
                }
            ]
          }
        }
        """.trimIndent().replace("#", "$")
        println(json)
        val tree: Expr = mapper.readValue(json)
         val obj = tree.call(emptyMap())
        println(mapper.writeValueAsString(obj))
        assertNotNull(obj)
    }

}