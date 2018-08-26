package io.em2m.search.core.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.xform.PushDownNotQueryTransformer
import io.em2m.search.core.xform.SimplifyQueryTransformer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryTransformerTest {

    val xform = SimplifyQueryTransformer()
    val notXform = PushDownNotQueryTransformer()

    fun Query.simplify() = xform.transform(this)
    fun Query.pushDownNot() = notXform.transform(this)

    @Test
    fun testSimplifyBool() {
        val tq = TermQuery("field", "value")
        val q1 = AndQuery(tq)
        val q2 = q1.simplify()
        assertEquals(tq, q2)
    }

    @Test
    fun testSimplifyOr() {
        val tq = TermQuery("field", "value")
        val q1 = OrQuery(tq)
        val q2 = q1.simplify()
        assertEquals(tq, q2)
    }

    @Test
    fun testSimplifyNot() {
        val tq = TermQuery("field", "value")
        val q1 = NotQuery(OrQuery(tq, tq))
        val q2 = q1.pushDownNot().simplify() as BoolQuery
        assertTrue((q2 is AndQuery))
        assertTrue(q2.of.first() is NotQuery)
        assertTrue(q2 is BoolQuery && q2.of.first() is NotQuery)
        val mapper = jacksonObjectMapper()
        println(mapper.writeValueAsString(q1))
        println(mapper.writeValueAsString(q2))
    }

}