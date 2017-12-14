package io.em2m.search.es

import com.scaleset.geo.Feature
import com.vividsolutions.jts.geom.Envelope
import io.em2m.search.core.model.*
import org.junit.Before
import org.junit.Test
import rx.observers.TestSubscriber
import kotlin.properties.Delegates

class EsSearchDaoQueryTest : FeatureTestBase() {

    var searchDao: SearchDao<Feature> by Delegates.notNull()

    @Before
    override fun before() {
        super.before()
        searchDao = EsSearchDao(esClient, "features", "feature", Feature::class.java, FeatureMapper())
    }

    @Test
    fun testMatchAll() {
        val request = SearchRequest(0, 20, MatchAllQuery())
        val sub = TestSubscriber<Any>()
        searchDao.search(request).doOnNext { result ->
            assertEquals(46, result.totalItems)
            assertEquals(20, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testLuceneQuery() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 5, LuceneQuery("properties.mag:[4.0 TO *]"))
        searchDao.search(request).doOnNext { result ->
            assertEquals(16, result.totalItems)
            assertEquals(5, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testRange() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 5, RangeQuery("properties.mag", gte = 4))
        searchDao.search(request).doOnNext { result ->
            assertEquals(16, result.totalItems)
            assertEquals(5, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testTerm() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, TermQuery("properties.mag", value = "2.5"))
        searchDao.search(request).doOnNext { result ->
            assertEquals(3, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testMatch() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, MatchQuery("properties.place", value = "alaska"))
        searchDao.search(request).doOnNext { result ->
            assertEquals(10, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testPrefix() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, PrefixQuery("id", value = "nn"))
        searchDao.search(request).doOnNext { result ->
            assertEquals(13, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testBool() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, OrQuery(listOf(TermQuery("properties.mag", "2.5"), TermQuery("properties.mag", 5.2))))
        searchDao.search(request).doOnNext { result ->
            assertEquals(4, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testBbox() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, BboxQuery("geometry.coordinates", Envelope(-180.0, 180.0, -90.0, 90.0)))
        searchDao.search(request).doOnNext { result ->
            assertEquals(46, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testExists() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, ExistsQuery("properties.alert"))
        searchDao.search(request).doOnNext { result ->
            assertEquals(3, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testNotExists() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 0, ExistsQuery("properties.alert", false))
        searchDao.search(request).doOnNext { result ->
            assertEquals(43, result.totalItems)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

}


