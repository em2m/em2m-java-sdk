package io.em2m.simplex.std

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.*
import io.em2m.simplex.parser.SimplexModule
import io.em2m.utils.coerce
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class ObjectsTest {

    val simplex = Simplex()
    val keys = BasicKeyResolver(
        mapOf(
            Key("f", "*") to (PathKeyHandler("fieldValues") as KeyHandler),
            Key("field", "*") to (PathKeyHandler("fieldValues") as KeyHandler)
        )
    )

    init {
        simplex.keys(keys)
    }

    @Test
    fun testSingleObjectPath() {
        val data = mapOf("foo" to "bar1", "foo2" to "bar2")
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val bar1 = simplex.eval("\${f:data | path:foo}", context)
        val bar2 = simplex.eval("\${f:data | path:foo2}", context)
        assertEquals(bar1, "bar1")
        assertEquals(bar2, "bar2")
    }

    @Test
    fun testSingleObjectWithPath() {
        val data = mapOf(
            "taco" to mapOf("foo" to "bar1", "foo2" to "bar2"),
            "pizza" to mapOf("foo" to "bar3", "foo2" to "bar4")
        )
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val bar1 = simplex.eval("\${f:data | path:taco.foo}", context)
        val bar2 = simplex.eval("\${f:data | path:taco.foo2}", context)
        val bar3 = simplex.eval("\${f:data | path:pizza.foo}", context)
        val bar4 = simplex.eval("\${f:data | path:pizza.foo2}", context)
        assertEquals(bar1, "bar1")
        assertEquals(bar2, "bar2")
        assertEquals(bar3, "bar3")
        assertEquals(bar4, "bar4")
    }

    @Test
    fun testSingleObjectWithPathBy() {
        val data = mapOf(
            "taco" to mapOf("foo" to "bar1", "foo2" to "bar2"),
            "pizza" to mapOf("foo" to "bar3", "foo2" to "bar4")
        )
        val context = mapOf(
            "fieldValues" to mapOf("data" to data),
            "taco" to "taco.foo", "pizza" to "pizza.foo"
        )
        val bar1 = simplex.eval("\${f:data | pathBy:taco}", context)
        val bar2 = simplex.eval("\${f:data | pathBy:pizza}", context)
        assertEquals("bar1", bar1)
        assertEquals("bar3", bar2)
    }


    @Test
    fun testListObjectPath() {
        val data = listOf(
            mapOf("foo" to "bar1", "foo2" to "bar2"),
            mapOf("foo" to "bar3", "foo2" to "bar4"),
            mapOf("foo" to "bar5", "foo2" to "bar6")
        )
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val bar135 = simplex.eval("\${f:data | path:foo}", context)
        val bar246 = simplex.eval("\${f:data | path:foo2}", context)
        assertEquals(bar135, listOf("bar1", "bar3", "bar5"))
        assertEquals(bar246, listOf("bar2", "bar4", "bar6"))
    }

    @Test
    fun testListObjectWithPath() {
        val data = listOf(
            mapOf("taco" to mapOf("foo" to "bar1", "foo2" to "bar2")),
            mapOf("taco" to mapOf("foo" to "bar3", "foo2" to "bar4"))
        )
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val bar13 = simplex.eval("\${f:data | path:taco.foo}", context)
        val bar24 = simplex.eval("\${f:data | path:taco.foo2}", context)
        assertEquals(bar13, listOf("bar1", "bar3"))
        assertEquals(bar24, listOf("bar2", "bar4"))
    }

    @Test
    fun testListObjectPathWithJoin() {
        val data = listOf(
            mapOf("foo" to "bar1", "foo2" to "bar2"),
            mapOf("foo" to "bar3", "foo2" to "bar4"),
            mapOf("foo" to "bar5", "foo2" to "bar6")
        )
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val bar135 = simplex.eval("\${f:data | path:foo | join}", context)
        val bar246 = simplex.eval("\${f:data | path:foo2 | join}", context)
        assertEquals("bar1, bar3, bar5", bar135)
        assertEquals("bar2, bar4, bar6", bar246)
    }

    @Test
    fun testListObjectWithPathAndJoin() {
        val data = listOf(
            mapOf("taco" to mapOf("foo" to "bar1", "foo2" to "bar2")),
            mapOf("taco" to mapOf("foo" to "bar3", "foo2" to "bar4"))
        )
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val bar13 = simplex.eval("\${f:data | path:taco.foo | join}", context)
        val bar24 = simplex.eval("\${f:data | path:taco.foo2 | join}", context)
        assertEquals(bar13, "bar1, bar3")
        assertEquals(bar24, "bar2, bar4")
    }

    @Test
    fun testSingleObjectEntries() {
        val data = mapOf("foo" to "bar1", "foo2" to "bar2")
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val entries: List<Map<*, *>> = simplex.eval("\${f:data | entries}", context).coerce() ?: emptyList()
        assertEquals(2, entries.size)
        assertEquals("foo", entries[0]["key"])
        assertEquals("foo2", entries[1]["key"])
        //assertEquals(data.entries.toTypedArray(), entries)
    }

    @Test
    fun testSingleObjectEntriesJoin() {
        val data = mapOf("foo" to "bar1", "foo2" to "bar2")
        val context = mapOf("fieldValues" to mapOf("data" to data))
        val result = simplex.eval("\${f:data | entries | path:key | join}", context).toString()
        assertEquals("foo, foo2", result)
    }

    @Test
    fun testPair() {
        val exec: Expr = jacksonObjectMapper().registerModule(SimplexModule(simplex)).readValue(
            """
                  {
                    "@container": [
                      {
                         "@exec": "object:pair",
                         "key": "1_#{key}",
                         "value": "#{value}"
                       },
                      {
                         "@exec": "object:pair",
                         "key": "2_#{key}",
                         "value": "#{value}"
                      }
                    ]
                  }
                """.replace("#", "$")
        )
        val data = mapOf("key" to "x", "value" to "y")
        val context = mapOf("fieldValues" to data)
        val result = exec.call(context)
        Assert.assertEquals(mapOf("1_x" to "y", "2_x" to "y"), result)
    }

}
