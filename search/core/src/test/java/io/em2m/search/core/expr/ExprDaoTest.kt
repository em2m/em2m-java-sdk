package io.em2m.search.core.expr

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.em2m.search.core.model.*
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.BasicPipeTransformResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.std.Numbers
import io.em2m.simplex.std.Strings
import org.junit.Assert
import org.junit.Test
import rx.Observable


class ExprDaoTest : Assert() {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("bucket", "key") to BucketKeyKeyHandler(),
            Key("f", "*") to FieldKeyHandler()))

    val pipeResolver = BasicPipeTransformResolver()
            .delegate(Numbers.pipes)
            .delegate(Strings.pipes)

    val request = SearchRequest()

    val mock = mock<SearchDao<Any>> {
        val rows = listOf(
                listOf("fred", "flinstone"),
                listOf("wilma", "flinstone"),
                listOf("barney", "rubble"),
                listOf("betty", "rubble")
        )
        val aggResults = mapOf(
                "lastName" to AggResult(key = "lastName", buckets = listOf(
                        Bucket(key = "flinstone", count = 2),
                        Bucket(key = "rubble", count = 2)
                )))
        on { search(any()) } doReturn Observable.just(SearchResult<Any>(rows = rows, aggs = aggResults, totalItems = rows.size.toLong()))
    }

    val dao = ExprTransformingSearchDao(keyResolver, pipeResolver, mock)


    @Test
    fun testTransformRows() {

        val request = SearchRequest(fields = listOf(Field(expr = "#{f:firstName | capitalize} #{f:lastName | capitalize}".replace("#", "$"))))
        val result = dao.search(request).toBlocking().first()
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
        val result = dao.search(request).toBlocking().first()
        assertNotNull(result)

        val aggs = result.aggs
        assertNotNull(aggs)

        val buckets = requireNotNull(aggs["lastName"]?.buckets)
        assertEquals("Flinstone", buckets[0].label)
        assertEquals("Rubble", buckets[1].label)
    }

}