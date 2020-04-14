package io.em2m.search.core.xform

import io.em2m.search.core.expr.FieldKeyHandler
import io.em2m.search.core.model.*
import io.em2m.search.core.xform.FieldTransformer.FieldModel
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldTransformerTest {

    private val simplex = Simplex()
            .keys(BasicKeyResolver().key(Key("field", "*"), FieldKeyHandler()))

    private val subTitleExpr = simplex.parser.parse("\${type}:\${summary}")

    private val xform = FieldTransformer<Any>(simplex, listOf(
            FieldModel("_title", delegateField = "name"),
            FieldModel("_title", delegateField = "name"),
            FieldModel("_subtitle", expr = subTitleExpr)))

    @Test
    fun testSingleFieldAlias() {
        val req = SearchRequest(fields = listOf(Field("_title")), sorts = listOf(DocSort("_title")))
        val req2 = xform.transformRequest(req)
        assertEquals("name", req2.fields.first().name)
        assertEquals("name", req2.sorts.first().field)
    }

    @Test
    fun testMultiFieldAlias() {
        val req = SearchRequest(fields = listOf(Field("_subtitle")), sorts = listOf(DocSort("_subtitle")))
        val req2 = xform.transformRequest(req)
        assertEquals(listOf("type", "summary"), req2.fields.map { it.name })
        assertEquals(listOf("type", "summary"), req2.sorts.map { it.field })
    }

    @Test
    fun testSingleFieldQuery() {
        val req = SearchRequest(query = TermQuery("_title", "Title"))
        val req2 = xform.transformRequest(req)
        val q2 = req2.query
        assertTrue(q2 is TermQuery)
        assertEquals("name", q2.field)
        assertEquals("Title", q2.value)
    }

    @Test
    fun testMultiFieldQuery() {
        val req = SearchRequest(query = TermQuery("_subtitle", "Summary"))
        val req2 = xform.transformRequest(req)
        val q2 = req2.query
        assertTrue(q2 is OrQuery)
        assertEquals(2, q2.of.size)
        val tq1 = q2.of[0] as TermQuery
        val tq2 = q2.of[1] as TermQuery
        assertEquals("type", tq1.field)
        assertEquals("summary", tq2.field)
    }

    @Test
    fun testTransformAggs() {
        val req = SearchRequest(aggs = listOf(TermsAgg("_title")))
        val req2 = xform.transformRequest(req)
        assertEquals(1, req2.aggs.size)
        val agg = req2.aggs.first() as TermsAgg
        assertEquals("name", agg.field)
    }

    @Test
    fun testTransformRows() {
        val req = SearchRequest(fields = listOf(Field(expr = "\${_title} / \${_subtitle}")))
        val rows = listOf(
                listOf("Name 1", "Type 1", "Summary 1"),
                listOf("Name 2", "Type 2", "Summary 2")
        )
        val res = SearchResult<Any>(rows = rows, totalItems = 2)
        val res2 = xform.transformResult(req, res)
        val rows2 = res2.rows ?: emptyList()
        assertEquals("Name 1 / Type 1:Summary 1", rows2[0][0])
        assertEquals("Name 2 / Type 2:Summary 2", rows2[1][0])
    }

    @Test
    fun testBucketLabel() {
    }

    @Test
    fun testTransformAggResults() {
    }

}