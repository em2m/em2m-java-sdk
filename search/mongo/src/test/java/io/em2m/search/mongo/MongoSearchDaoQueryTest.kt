package io.em2m.search.mongo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.vividsolutions.jts.geom.Envelope
import io.em2m.search.core.model.*
import org.junit.Assert
import org.junit.Test
import rx.observers.TestSubscriber

class MongoSearchDaoQueryTest : FeaturesTestBase() {

    @Test
    fun testMatchAll() {
        val request = SearchRequest(0, 20, MatchAllQuery())
        val sub = TestSubscriber<Any>()
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(46, result.totalItems)
            Assert.assertEquals(20, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testLuceneQuery() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 5, LuceneQuery("properties.mag:[4.0 TO *]"))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(16, result.totalItems)
            Assert.assertEquals(5, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testRange() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 5, RangeQuery("properties.mag", gte = 4))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(16, result.totalItems)
            Assert.assertEquals(5, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testDateRange() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 5, DateRangeQuery("properties.time", gte = "1408447319000", lte = "now"))
        searchDao.search(request).doOnNext { result ->
            // Shouldn't this be 21?
            Assert.assertEquals(20, result.totalItems)
            Assert.assertEquals(5, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testTerm() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, TermQuery("properties.mag", value = "2.5"))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(3, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testTerms() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, TermsQuery("properties.mag", value = listOf("2.5", "2.7")))
        searchDao.search(request).doOnNext { result ->
            assertEquals(6, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testMatch() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, MatchQuery("properties.place", value = "alaska"))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(10, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testWildcard() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, WildcardQuery("id", value = "nn*"))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(13, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }


    @Test
    fun testPrefix() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, PrefixQuery("id", value = "nn"))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(13, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testBool() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, OrQuery(TermQuery("properties.mag", "2.5"), TermQuery("properties.mag", 5.2)))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(4, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testNotBool() {
        val sub = TestSubscriber<Any>()
        val boolQuery = NotQuery(OrQuery(AndQuery(TermQuery("properties.mag", "2.5")), AndQuery(TermQuery("properties.mag", 5.2))))
        val request = SearchRequest(0, 0, boolQuery)
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(42, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testBbox() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, BboxQuery("geometry.coordinates", Envelope(-180.0, 180.0, -90.0, 90.0)))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(46, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testFields() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 1, query = TermQuery("properties.mag", 5.2), fields = listOf(Field("properties.mag")))
        searchDao.search(request).doOnNext { result ->
            val mag = requireNotNull(result?.rows?.get(0)?.get(0)) as Double
            Assert.assertEquals(5.2, mag, 0.01)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testNativeQuery() {
        val sub = TestSubscriber<Any>()
        val type = "\$type"
        val mongoQuery = jacksonObjectMapper().readTree(""" { "properties.nst": { "$type": 10 } } """)
        val request = SearchRequest(0, 1, query = NativeQuery(mongoQuery), fields = listOf(Field("properties.nst")))
        searchDao.search(request).doOnNext { result ->
            val nst = result?.rows?.get(0)?.get(0)
            Assert.assertEquals(null, nst)
            Assert.assertEquals(27, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testNullTermQuery() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 1, query = TermQuery("properties.nst", null), fields = listOf(Field("properties.nst")))
        searchDao.search(request).doOnNext { result ->
            val nst = result?.rows?.get(0)?.get(0)
            Assert.assertEquals(null, nst)
            Assert.assertEquals(27, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testStarMatchQuery() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, query = MatchQuery("properties.nst", "*"), fields = listOf(Field("properties.nst")))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(19, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testNotStarMatchQuery() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, query = NotQuery(MatchQuery("properties.nst", "*")), fields = listOf(Field("properties.nst")))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(27, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testExists() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, ExistsQuery("properties.alert"))
        searchDao.search(request).doOnNext { result ->
            Assert.assertEquals(3, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }


}
