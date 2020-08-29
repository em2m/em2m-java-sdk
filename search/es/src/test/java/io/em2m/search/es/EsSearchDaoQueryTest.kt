package io.em2m.search.es

import io.em2m.geo.feature.Feature
import io.em2m.search.core.model.*
import org.junit.Before
import org.junit.Test
import org.locationtech.jts.geom.Envelope
import kotlin.properties.Delegates

class EsSearchDaoQueryTest : FeatureTestBase() {

    private var searchDao: SyncDao<Feature> by Delegates.notNull()

    @Before
    override fun before() {
        super.before()
        searchDao = EsSyncDao(esClient, index, type, Feature::class.java, idMapper)
    }

    @Test
    fun testMatchAll() {
        val request = SearchRequest(0, 20, MatchAllQuery())
        val result = searchDao.search(request)
        assertEquals(46, result.totalItems)
        assertEquals(20, result.items?.size)
    }

    @Test
    fun testLuceneQuery() {
        val request = SearchRequest(0, 5, LuceneQuery("properties.mag:[4.0 TO *]"))
        val result = searchDao.search(request)
        assertEquals(16, result.totalItems)
        assertEquals(5, result.items?.size)
    }

    @Test
    fun testRange() {
        val request = SearchRequest(0, 5, RangeQuery("properties.mag", gte = 4))
        val result = searchDao.search(request)
        assertEquals(16, result.totalItems)
        assertEquals(5, result.items?.size)
    }

    @Test
    fun testDateRange() {
        val request = SearchRequest(0, 5, RangeQuery("properties.time", gte = "1408447319000", lte = "now"))
        val result = searchDao.search(request)
        assertEquals(21, result.totalItems)
        assertEquals(5, result.items?.size)
    }

    @Test
    fun testTerm() {
        val request = SearchRequest(0, 0, TermQuery("properties.mag", value = "2.5"))
        val result = searchDao.search(request)
        assertEquals(3, result.totalItems)
    }

    @Test
    fun testTerms() {
        val request = SearchRequest(0, 0, TermsQuery("properties.mag", value = listOf("2.5", "2.7")))
        val result = searchDao.search(request)
        assertEquals(6, result.totalItems)
    }


    @Test
    fun testMatch() {
        val request = SearchRequest(0, 0, MatchQuery("properties.place", value = "alaska"))
        val result = searchDao.search(request)
        assertEquals(10, result.totalItems)
    }

    @Test
    fun testWildcard() {
        val request = SearchRequest(0, 0, WildcardQuery("id", value = "nn*"))
        val result = searchDao.search(request)
        assertEquals(13, result.totalItems)
    }

    @Test
    fun testPrefix() {
        val request = SearchRequest(0, 0, PrefixQuery("id", value = "nn"))
        val result = searchDao.search(request)
        assertEquals(13, result.totalItems)
    }

    @Test
    fun testBool() {
        val request = SearchRequest(0, 0, OrQuery(listOf(TermQuery("properties.mag", "2.5"), TermQuery("properties.mag", 5.2))))
        val result = searchDao.search(request)
        assertEquals(4, result.totalItems)
    }

    @Test
    fun testBbox() {
        val request = SearchRequest(0, 0, BboxQuery("geometry.coordinates", Envelope(-180.0, 180.0, -90.0, 90.0)))
        val result = searchDao.search(request)
        assertEquals(46, result.totalItems)
    }

    @Test
    fun testExists() {
        val request = SearchRequest(0, 0, ExistsQuery("properties.alert"))
        val result = searchDao.search(request)
        assertEquals(3, result.totalItems)
    }

    @Test
    fun testNotExists() {
        val request = SearchRequest(0, 0, ExistsQuery("properties.alert", false))
        val result = searchDao.search(request)
        assertEquals(43, result.totalItems)
    }

}


