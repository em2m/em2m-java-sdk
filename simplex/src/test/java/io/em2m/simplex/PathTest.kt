package io.em2m.simplex

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
    data class B(val c: String)

    val bean = Bean(A(B(c = "value")))

    val json = jacksonObjectMapper().readTree("""
        {
          "a": {
            "b": {
              "c": "value"
            }
          },
          "d": "dval",
          "e": "eval"
        }
    """)

    val map: Map<String, *> = json.coerceNonNull()

    val path = "a.b.c"

    @Test
    fun testBeans() {
        val expr = PathExpr(path)
        val value = expr.call(bean)
        assertEquals("value", value)
    }

    @Test
    fun testJson() {
        val expr = PathExpr(path)
        val value = expr.call(json)
        assertEquals("value", value)
    }

    @Test
    fun testMap() {
        val expr = PathExpr(path)
        val value = expr.call(map)
        assertEquals("value", value)
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
        simplex.keys(BasicKeyResolver().key(Key("field", "*"), PathKeyHandler(simplex)))
        val expr = "\${a.b.c}"
        val context = map
        assertEquals("value", simplex.eval(expr, context))
    }

    @Test
    fun testKeyHandlerPrefix() {
        val simplex = Simplex()
        simplex.keys(BasicKeyResolver().key(Key("field", "*"), PathKeyHandler(simplex, "a")))
        val expr = "\${b.c}"
        val context = map
        assertEquals("value", simplex.eval(expr, context))
    }

    @Test
    fun testKeyHandlerMultiplePaths() {
        val simplex = Simplex()
        simplex.keys(BasicKeyResolver().key(Key("field", "*"), PathKeyHandler(simplex)))
        val expr = "\${a.b.c,d,e}"
        val context = map
        assertArrayEquals(arrayOf("value", "dval", "eval"), (simplex.eval(expr, context) as List<*>).toTypedArray())
    }


}