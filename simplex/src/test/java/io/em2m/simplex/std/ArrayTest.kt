package io.em2m.simplex.std

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import org.junit.Test
import kotlin.test.assertEquals


class ArrayTest {

    private val mapper = jacksonObjectMapper()
    private val test = mapper.createArrayNode().add("A").add("B").add("C")

    private val keyResolver = BasicKeyResolver(mapOf(
        Key("ns", "key1") to ConstKeyHandler(listOf("A", "B", "C")),
        Key("ns", "key2") to ConstKeyHandler("value2"),
        Key("ns", "pie") to ConstKeyHandler(3.14),
        Key("ns", "duration") to ConstKeyHandler(210_000),
        Key("ns", "five") to ConstKeyHandler(5),
        Key("ns", "arrayNode") to ConstKeyHandler(test),
        Key("ns", "set") to ConstKeyHandler(setOf("D", "E", "F")),
        Key("ns", "listOfMaps") to ConstKeyHandler(listOf(
            mapOf("id" to "1", "value" to "one"),
            mapOf("id" to "2", "value" to "two"),
            mapOf("id" to "3", "value" to "three")
        )),
        Key("ns", "setOfMaps") to ConstKeyHandler(setOf(
            mapOf("id" to "1", "value" to "one"),
            mapOf("id" to "2", "value" to "two"),
            mapOf("id" to "3", "value" to "three")
        ))
    ))
        .delegate(Numbers.keys)


    val simplex = Simplex().keys(keyResolver)

    @Test
    fun `Size of the array using a pipe`() {
        val result = simplex.eval("\${ns:key1 | size}", emptyMap())
        val result2 = simplex.eval("\${ns:set | size}", emptyMap())
        assertEquals(3, result)
        assertEquals(3, result2)
    }

    @Test
    fun `First item in the array`() {
        val result = simplex.eval("\${ns:key1 | first}", emptyMap())
        val result2 = simplex.eval("\${ns:set | first}", emptyMap())
        assertEquals("A", result)
        assertEquals("D", result2)
    }

    @Test
    fun `Last item in the array`() {
        val result = simplex.eval("\${ns:key1 | last}", emptyMap())
        val result2 = simplex.eval("\${ns:set | last}", emptyMap())
        assertEquals("C", result)
        assertEquals("F", result2)
    }

    @Test
    fun `Reversed items in the array`() {
        val result = simplex.eval("\${ns:key1 | reversed | first}", emptyMap())
        val result2 = simplex.eval("\${ns:set | reversed | first}", emptyMap())
        assertEquals("C", result)
        assertEquals("F", result2)
    }

    @Test
    fun testSlice() {
        val result1 = simplex.eval("\${ns:key1 | slice:0:0}", emptyMap())
        val result2 = simplex.eval("\${ns:key1 | slice:0:1}", emptyMap())
        val result3 = simplex.eval("\${ns:key1 | slice:1:1}", emptyMap())
        val result4 = simplex.eval("\${ns:set | slice:1:1}", emptyMap())
        assertEquals(listOf("A"), result1)
        assertEquals(listOf("A", "B"), result2)
        assertEquals(listOf("B"), result3)
        assertEquals(listOf("E"), result4)
    }

    @Test
    fun testTake() {
        val result1 = simplex.eval("\${ns:key1 | take:2}", emptyMap())
        val result2 = simplex.eval("\${ns:key2 | take:3}", emptyMap())
        val result3 = simplex.eval("\${ns:arrayNode | take:2}", emptyMap())
        val result4 = simplex.eval("\${ns:set | take:2}", emptyMap())
        assertEquals(listOf("A", "B"), result1)
        assertEquals("val", result2)
        assertEquals(listOf("D", "E"), result4)
        //assertEquals(mapper.createArrayNode().add("A").add("B"), result3)
    }

    @Test
    fun testTakeLast() {
        val result1 = simplex.eval("\${ns:key1 | takeLast:2}", emptyMap())
        val result2 = simplex.eval("\${ns:key2 | takeLast:3}", emptyMap())
        val result3 = simplex.eval("\${ns:arrayNode | takeLast:2}", emptyMap())
        val result4 = simplex.eval("\${ns:set | takeLast:2}", emptyMap())
        assertEquals(listOf("B", "C"), result1)
        assertEquals("ue2", result2)
        assertEquals(listOf("B", "C"), result3)
        assertEquals(listOf("E", "F"), result4)
    }

    @Test
    fun testFilter() {
        val listResult1 = simplex.eval("\${ns:listOfMaps | filter:id:1}", emptyMap())
        val listResult2 = simplex.eval("\${ns:listOfMaps | filter:id:4}", emptyMap())
        val setResult1 = simplex.eval("\${ns:setOfMaps | filter:id:1}", emptyMap())
        val setResult2 = simplex.eval("\${ns:setOfMaps | filter:id:4}", emptyMap())
        assertEquals(listOf(mapOf("id" to "1", "value" to "one")), listResult1)
        assertEquals(emptyList<Any>(), listResult2)
        assertEquals(listOf(mapOf("id" to "1", "value" to "one")), setResult1)
        assertEquals(emptyList<Any>(), setResult2)
    }

}
