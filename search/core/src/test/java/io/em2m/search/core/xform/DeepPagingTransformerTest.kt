package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepPagingTransformerTest {

    private val xform = DeepPagingTransformer<Any?>("_id")

    @Test
    fun testMoSortsNoLast() {
        val req = SearchRequest(
            query = MatchAllQuery(),
            params = mapOf("deepPage" to true),
            fields = listOf(Field("name"), Field("title"))
        )
        val req2 = xform.transformRequest(req, emptyMap())
        val q2 = req2.query
        assertTrue(q2 is MatchAllQuery)
    }

    @Test
    fun testMoSortsWithLast() {
        val params = mapOf("lastKey" to mapOf("_id" to 1), "deepPage" to true)
        val req = SearchRequest(
            query = MatchAllQuery(),
            fields = listOf(Field("name"), Field("title")),
            params = params
        )
        val req2 = xform.transformRequest(req, emptyMap())
        val q2 = req2.query
        assertTrue(q2 is RangeQuery)
        assertEquals(1, q2.gt)
    }

    @Test
    fun `request with one sort and a last value`() {
        val idField = "_id"
        val titleField = "title"
        val idValue = 1
        val titleValue = "Hello"

        val params = mapOf("lastKey" to mapOf(idField to idValue, titleField to titleValue), "deepPage" to true)
        val req = SearchRequest(
            query = MatchAllQuery(),
            sorts = listOf(DocSort(titleField, direction = Direction.Descending)),
            fields = listOf(Field("name"), Field(titleField)),
            params = params
        )
        val result = xform.transformRequest(req, emptyMap()).query

        assertTrue(result is OrQuery)
        assertEquals(2, (result as OrQuery).of.size)

        // First query: (_id > 1) AND (title == "Hello")
        val firstQuery = result.of[0] as AndQuery
        assertEquals(idField, (firstQuery.of[0] as RangeQuery).field)
        assertEquals(idValue, (firstQuery.of[0] as RangeQuery).gt)
        assertEquals(titleField, (firstQuery.of[1] as TermQuery).field)
        assertEquals(titleValue, (firstQuery.of[1] as TermQuery).value)

        // Second query: (title < "Hello")
        val secondQuery = result.of[1] as RangeQuery
        assertEquals(titleField, secondQuery.field)
        assertEquals(titleValue, secondQuery.lt)
    }

    @Test
    fun `request with null value in lastKey should skip that field`() {
        val id = 321
        val params = mapOf(
            "lastKey" to mapOf("_id" to id, "title" to null),
            "deepPage" to true
        )
        val req = SearchRequest(
            query = MatchAllQuery(),
            sorts = listOf(DocSort("title")),
            fields = listOf(Field("name"), Field("title")),
            params = params
        )
        val req2 = xform.transformRequest(req, emptyMap())
        val q2 = req2.query
        assertTrue(q2 is RangeQuery)
        assertEquals(id, (q2 as RangeQuery).gt)
    }

    @Test
    fun `request with all null values in lastKey should return original query`() {
        val params = mapOf(
            "lastKey" to mapOf("_id" to null, "title" to null),
            "deepPage" to true
        )
        val req = SearchRequest(
            query = MatchAllQuery(),
            sorts = listOf(DocSort("title")),
            fields = listOf(Field("name"), Field("title")),
            params = params
        )
        val req2 = xform.transformRequest(req, emptyMap())
        assertTrue(req2.query is MatchAllQuery)
    }

    @Test
    fun `request with missing sort fields in lastKey uses only available fields`() {
        val id = 321
        val params = mapOf(
            "lastKey" to mapOf("_id" to id),
            "deepPage" to true
        )
        val req = SearchRequest(
            query = MatchAllQuery(),
            sorts = listOf(DocSort("title"), DocSort("category")),
            fields = listOf(Field("name"), Field("title")),
            params = params
        )
        val req2 = xform.transformRequest(req, emptyMap())
        val q2 = req2.query
        assertTrue(q2 is RangeQuery)
        assertEquals(id, (q2 as RangeQuery).gt)
    }

    @Test
    fun `request with empty lastKey map returns original query`() {
        val params = mapOf(
            "lastKey" to emptyMap<String, Any?>(),
            "deepPage" to true
        )
        val req = SearchRequest(
            query = MatchAllQuery(),
            sorts = listOf(DocSort("title")),
            fields = listOf(Field("name"), Field("title")),
            params = params
        )
        val req2 = xform.transformRequest(req, emptyMap())
        assertTrue(req2.query is MatchAllQuery)
    }

}
