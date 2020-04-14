package io.em2m.search.core.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert
import org.junit.Test

class QuerySimplifyTest {

    @Test
    fun testSimplifyBool() {
        val tq = TermQuery("field", "value")
        val q1 = AndQuery(tq, MatchAllQuery())
        val q2 = q1.simplify()
        Assert.assertEquals(tq, q2)
    }

    @Test
    fun testSimplifyOr() {
        val tq = TermQuery("field", "value")
        val q1 = OrQuery(tq)
        val q2 = q1.simplify()
        Assert.assertEquals(tq, q2)
    }

}