package io.em2m.simplex

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.PathExpr
import io.em2m.simplex.model.PathKeyHandler
import io.em2m.utils.coerceNonNull
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test


class PathTest {

    data class Bean(val a: A)
    data class A(val b: B)
    data class B(var c: String)

    val bean = Bean(A(B(c = "value")))

    val json = jacksonObjectMapper().readTree(
        """
        {
          "a": {
            "b": {
              "c": "value",
              "d": ["0", "1", "2"]
            }
          },
          "d": "dval",
          "e": "eval",
          "f": ["a", "b", "c"],
          "g": [ { "x" : 1}, { "x": 2} ],
          "h": null
        }
    """
    )

    val node = ObjectMapper().readTree(
        """
        {
          "a": {
            "b": {
              "c": "value",
              "d": ["0", "1", "2"]
            }
          },
          "d": "dval",
          "e": "eval",
          "f": ["a", "b", "c"],
          "g": [ { "x" : 1}, { "x": 2} ],
          "h": null
        }
    """
    )

    val map: Map<String, *> = json.coerceNonNull()

    val path = "a.b.c"

    @Test
    fun testBeans() {
        val expr = PathExpr(path)
        val value = expr.call(bean)
        assertEquals("value", value)

        expr.setValue(bean, "value2")
        assertEquals("value2", expr.call(bean))

    }

    @Test
    fun testJson() {
        val expr = PathExpr(path)
        val value = expr.call(json)
        assertEquals("value", value)

        expr.setValue(json, "value2")
        assertEquals("value2", expr.call(json))
    }

    @Test
    fun testNode() {
        val expr = PathExpr(path)
        val value = expr.call(node)
        assertEquals("value", value)

        val expr1 = PathExpr("a.b.d")
        val value1 = expr1.call(node)
        assertEquals(listOf("0", "1", "2"), value1)
    }

    @Test
    fun testMap() {
        val expr = PathExpr(path)
        val value = expr.call(map)
        assertEquals("value", value)

        expr.setValue(map, "value2")
        assertEquals("value2", expr.call(map))
    }

    @Test
    fun testSingletonMap() {
        val expr = PathExpr("a.b")
        val map = mapOf("a" to (mapOf("b" to "value")))
        val value = expr.call(map)
        assertEquals("value", value)
    }

    @Test
    fun testPutMissing() {
        val expr = PathExpr("a.b.c")
        val obj = HashMap<String, Any?>()
        expr.setValue(obj, "value")
        val value = expr.call(obj)
        assertEquals("value", value)
    }

    @Test
    fun testAdd() {
        val expr = PathExpr("f")
        expr.addValue(map, listOf("addedValue", "anotherAddedValue"))

        assertEquals(listOf("a", "b", "c", "addedValue", "anotherAddedValue"), expr.call(map))
    }

    @Test
    fun testNestedAdd() {
        val expr = PathExpr("a.b.d")
        expr.addValue(map, "addedValue")

        assertEquals(listOf("0", "1", "2", "addedValue"), expr.call(map))
    }

    @Test
    fun testAddToNull() {
        val expr = PathExpr("h")
        expr.addValue(map, listOf("addedValue", "anotherAddedValue"))

        assertEquals(listOf("addedValue", "anotherAddedValue"), expr.call(map))
    }

    @Test
    fun testRemove() {
        val expr = PathExpr("a.b.c")
        expr.removeValue(map)
        val value = expr.call(map)
        assertEquals(null, value)
    }

    @Test
    fun testArray() {
        val expr = PathExpr("f.1")
        val value = expr.call(map)
        assertEquals("b", value)

        expr.setValue(map, "B")
        assertEquals("B", expr.call(map))
    }

    @Test
    fun testArrayNested() {
        val expr = PathExpr("g.x")
        val value = expr.call(map)
        assertEquals(listOf(1,2), value)
    }

    @Test
    fun testSimplex() {
        val simplex = Simplex()
        assertEquals("value", simplex.getPath(path, bean))
        assertEquals("value", simplex.getPath(path, map))
        assertEquals("value", simplex.getPath(path, json))
    }

    @Test
    fun testKeyHandler() {
        val simplex = Simplex()
        simplex.keys(BasicKeyResolver().key(Key("field", "*"), PathKeyHandler()))
        val expr = "\${a.b.c}"
        val context = map
        assertEquals("value", simplex.eval(expr, context))
    }

    @Test
    fun testKeyHandlerPrefix() {
        val simplex = Simplex()
        simplex.keys(BasicKeyResolver().key(Key("field", "*"), PathKeyHandler("a")))
        val expr = "\${b.c}"
        val context = map
        assertEquals("value", simplex.eval(expr, context))
    }

    @Test
    fun testKeyHandlerMultiplePaths() {
        val simplex = Simplex()
        simplex.keys(BasicKeyResolver().key(Key("field", "*"), PathKeyHandler()))
        val expr = "\${a.b.c,d,e}"
        val context = map
        assertArrayEquals(arrayOf("value", "dval", "eval"), (simplex.eval(expr, context) as List<*>).toTypedArray())
    }


}
