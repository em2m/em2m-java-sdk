package io.em2m.search.core.expr

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.em2m.search.core.model.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import org.junit.Assert
import org.junit.Test


class ExprDaoTest : Assert() {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("bucket", "key") to BucketKeyKeyHandler(),
            Key("f", "*") to FieldKeyHandler(),
            Key("field", "*") to FieldKeyHandler()))

    val request = SearchRequest()

    private val mock = mock<SyncDao<Any>> {
        val rows = listOf(
                listOf("fred", "flinstone"),
                listOf("wilma", "flinstone"),
                listOf("barney", "rubble"),
                listOf("betty", "rubble")
        )
        val aggResults = mapOf(
                "lastName" to AggResult(key = "lastName", op = "terms", field = "lastName", buckets = listOf(
                        Bucket(key = "flinstone", count = 2),
                        Bucket(key = "rubble", count = 2)
                )))
        on { search(any()) } doReturn SearchResult(rows = rows, aggs = aggResults, totalItems = rows.size.toLong())
    }

    private val simplex = Simplex().keys(keyResolver)
    private val dao = ExprTransformingSyncDao(simplex, mock)

    @Test
    fun testTransformRows() {

        val request = SearchRequest(fields = listOf(Field(expr = "#{f:firstName | capitalize} #{f:lastName | capitalize}".replace("#", "$"))))
        val result = dao.search(request)
        val rows = requireNotNull(result.rows)
        assertEquals("Fred Flinstone", rows[0][0])
        assertEquals("Wilma Flinstone", rows[1][0])
        assertEquals("Barney Rubble", rows[2][0])
        assertEquals("Betty Rubble", rows[3][0])
        assertNotNull(result)
    }

    @Test
    fun testTransformAggs() {
        val format = "#{bucket:key | capitalize}".replace("#", "$")
        val request = SearchRequest(aggs = listOf(TermsAgg(field = "lastName", key = "lastName", format = format)))
        val result = dao.search(request)
        assertNotNull(result)

        val aggs = result.aggs
        assertNotNull(aggs)

        val buckets = requireNotNull(aggs["lastName"]?.buckets)
        assertEquals("Flinstone", buckets[0].label)
        assertEquals("Rubble", buckets[1].label)
    }

    @Test
    fun testSourceFormatAggs() {
        val sourceFormat = "\${bucket:key | capitalize}"
        val format = "\${bucket:key | upperCase}"
        val request = SearchRequest(aggs = listOf(TermsAgg(field = "lastName", key = "lastName", format = format, ext = mapOf("sourceFormat" to sourceFormat))))
        val result = dao.search(request)
        assertNotNull(result)

        val aggs = result.aggs
        assertNotNull(aggs)

        val buckets = requireNotNull(aggs["lastName"]?.buckets)
        assertEquals("FLINSTONE", buckets[0].label)
    }

    @Test
    fun testTransformSorts() {
        val sorts = listOf(DocSort(field = "\${lastName}, \${firstName}"))
        val request = SearchRequest(sorts = sorts)
        val result = dao.search(request)
        assertNotNull(result)
    }

    @Test
    fun testTransformQuery() {
        val request = SearchRequest(query = TermQuery("lastName", "Flinstone"))
        val result = dao.search(request)
        assertNotNull(result)
    }

}