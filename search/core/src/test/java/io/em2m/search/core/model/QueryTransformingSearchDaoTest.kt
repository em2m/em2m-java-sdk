package io.em2m.search.core.model

import com.nhaarman.mockito_kotlin.*
import io.em2m.search.core.daos.QueryTransformingSyncDao
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.locationtech.jts.geom.Envelope
import org.mockito.Mockito.verify

class QueryTransformingSearchDaoTest : Assert() {

    val mock = mock<SyncDao<Any>> {
        on { search(any()) } doReturn SearchResult<Any>(totalItems = 0L)
    }

    @Test
    fun testFieldSet() {
        val xformDao = QueryTransformingSyncDao(fieldSets = mapOf("brief" to listOf(Field("id"), Field("name"))), delegate = mock)
        // known fieldSet
        xformDao.search(SearchRequest(fieldSet = "brief", fields = listOf(Field("other"))))
        // unknown fieldSet
        xformDao.search(SearchRequest(fieldSet = "unknown", fields = listOf(Field("other"))))
        // no fieldSet
        xformDao.search(SearchRequest(fields = listOf(Field("other"))))

        argumentCaptor<SearchRequest>().apply {
            verify(mock, times(3)).search(capture())

            // known fieldSet
            assertTrue(firstValue.fields.contains(Field("id")))
            assertTrue(firstValue.fields.contains(Field("name")))
            assertTrue(firstValue.fields.contains(Field("other")))
            assertNull(firstValue.fieldSet)

            // unknown fieldSet
            assertEquals("unknown", secondValue.fieldSet)
            assertEquals(1, secondValue.fields.size)
            assertTrue(firstValue.fields.contains(Field("other")))


            // no fieldSet
            assertNull(thirdValue.fieldSet)
            assertEquals(1, thirdValue.fields.size)
            assertTrue(firstValue.fields.contains(Field("other")))
        }
    }

    @Test
    fun testQueryFieldAlias() {
        val actual = "geometry.coordinates"
        val xformDao = QueryTransformingSyncDao(aliases = mapOf("coord" to Field(actual)), delegate = mock)

        xformDao.search(SearchRequest(fields = listOf(Field("coord"), Field("other"))))
        xformDao.search(SearchRequest(query = BboxQuery("coord", Envelope(-180.0, 180.0, -90.0, 90.0))))
        xformDao.search(SearchRequest(query = LuceneQuery("coord: 180")))
        xformDao.search(SearchRequest(query = AndQuery(listOf(TermQuery("coord", 79.0)))))

        argumentCaptor <SearchRequest>().apply {
            verify(mock, times(4)).search(capture())

            assertEquals(2, firstValue.fields.size)

            // first
            assertTrue(firstValue.fields.contains(Field(actual)))
            assertFalse(firstValue.fields.contains(Field("coord")))
            assertTrue(firstValue.fields.contains(Field("other")))

            allValues.forEach {
                when (val q = it.query) {
                    is FieldedQuery -> {
                        assertEquals(actual, q.field)
                    }
                    is TermQuery -> {
                        assertEquals(actual, q.field)
                        // from lucene, so value is string since we don't have a schema yet
                        assertEquals("70", q.value)
                    }
                    is AndQuery -> {
                        val term = q.of[0] as TermQuery
                        assertEquals(actual, term.field)
                    }
                }
            }
        }
    }

    @Test
    fun testLuceneParser() {
        val xformDao = QueryTransformingSyncDao(delegate = mock)
        xformDao.search(SearchRequest(query = LuceneQuery("make:Honda AND model:Accord")))

        argumentCaptor <SearchRequest>().apply {
            verify(mock, times(1)).search(capture())

            val q = firstValue.query as AndQuery
            assertTrue(q.of[0] is TermQuery)
            val make = q.of[0] as TermQuery
            assertEquals("Honda", make.value)
        }
    }

    @Test
    @Ignore
    fun testNamedFilter() {
        throw NotImplementedError()
    }

    @Test
    fun testNamedAgg() {
        val format = "#{bucket:key | upperCase}".replace("#", "$")
        val statusQuery = TermsAgg("status", format = format, ext = mapOf("icon" to "fa-key"))
        val xformDao = QueryTransformingSyncDao   (namedAggs = mapOf("statusAgg" to statusQuery), delegate = mock)
        xformDao.search(SearchRequest(aggs = listOf(NamedAgg(name = "statusAgg", key = "key").setAny("size", 5))))

        argumentCaptor <SearchRequest>().apply {
            verify(mock, times(1)).search(capture())

            val aggs = firstValue.aggs
            assertEquals(1, aggs.size)
            val status = aggs[0]
            assertNotNull(status)
            assertEquals("key", status.key)
            assertEquals("fa-key", status.extensions["icon"])
            if (status is TermsAgg) {
                assertEquals(format, status.format)
                assertEquals("status", status.field)
                assertEquals(5, status.size)
            } else {
                error("Agg is not a term agg")
            }
        }
    }

    @Test
    @Ignore
    fun testExtraFilters() {
        // example: token-based org filter
        // header: includeArchived: true
        throw NotImplementedError()
    }

    @Test
    @Ignore
    fun testCustomFilters() {
        // archived = false
        // orgPath / organization (sub-orgs)
        // hyphen problem for serialNumber
        // _all query?
        // vehicle_id in auth token
    }

}