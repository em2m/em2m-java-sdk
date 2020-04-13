package io.em2m.search.mongo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.model.*
import org.junit.Test
import org.locationtech.jts.geom.Envelope

class MongoSyncDaoQueryTest : FeaturesTestBase() {

    @Test
    fun testMatchAll() {
        val request = SearchRequest(0, 20, MatchAllQuery())
        val result = syncDao.search(request)
        assertEquals(46, result.totalItems)
        assertEquals(20, result.items?.size)
    }

    @Test
    fun testLuceneQuery() {
        val request = SearchRequest(0, 5, LuceneQuery("properties.mag:[4.0 TO *]"))
        val result = syncDao.search(request)
        assertEquals(16, result.totalItems)
        assertEquals(5, result.items?.size)
    }

    @Test
    fun testRange() {
        val request = SearchRequest(0, 5, RangeQuery("properties.mag", gte = 4))
        val result = syncDao.search(request)
        assertEquals(16, result.totalItems)
        assertEquals(5, result.items?.size)
    }

    @Test
    fun testDateRange() {
        val request = SearchRequest(0, 5, DateRangeQuery("properties.time", gte = "1408447319000", lte = "now"))
        val result = syncDao.search(request)
        // Shouldn't this be 21?
        assertEquals(20, result.totalItems)
        assertEquals(5, result.items?.size)
    }

    @Test
    fun testTerm() {
        val request = SearchRequest(0, 0, TermQuery("properties.mag", value = "2.5"))
        val result = syncDao.search(request)
        assertEquals(3, result.totalItems)
    }

    @Test
    fun testMatch() {
        val request = SearchRequest(0, 0, MatchQuery("properties.place", value = "alaska"))
        val result = syncDao.search(request)
        assertEquals(10, result.totalItems)
    }

    @Test
    fun testPrefix() {
        val request = SearchRequest(0, 0, PrefixQuery("id", value = "nn"))
        val result = syncDao.search(request)
        assertEquals(13, result.totalItems)
    }

    @Test
    fun testBool() {
        val request = SearchRequest(0, 0, OrQuery(TermQuery("properties.mag", "2.5"), TermQuery("properties.mag", 5.2)))
        val result = syncDao.search(request)
        assertEquals(4, result.totalItems)
    }

    @Test
    fun testBbox() {
        val request = SearchRequest(0, 0, BboxQuery("geometry.coordinates", Envelope(-180.0, 180.0, -90.0, 90.0)))
        val result = syncDao.search(request)
        assertEquals(46, result.totalItems)
    }

    @Test
    fun testFields() {
        val request = SearchRequest(0, 1, query = TermQuery("properties.mag", 5.2), fields = listOf(Field("properties.mag")))
        val result = syncDao.search(request)
        val mag = requireNotNull(result.rows?.get(0)?.get(0)) as Double
        assertEquals(5.2, mag, 0.01)
    }

    @Test
    fun testNativeQuery() {
        val type = "\$type"
        val mongoQuery = jacksonObjectMapper().readTree(""" { "properties.nst": { "$type": 10 } } """)
        val request = SearchRequest(0, 1, query = NativeQuery(mongoQuery), fields = listOf(Field("properties.nst")))
        val result = syncDao.search(request)
        val nst = result.rows?.get(0)?.get(0)
        assertEquals(null, nst)
        assertEquals(27, result.totalItems)
    }

    @Test
    fun testNullTermQuery() {
        val request = SearchRequest(0, 1, query = TermQuery("properties.nst", null), fields = listOf(Field("properties.nst")))
        val result = syncDao.search(request)
        val nst = result.rows?.get(0)?.get(0)
        assertEquals(null, nst)
        assertEquals(27, result.totalItems)
    }

    @Test
    fun testStarMatchQuery() {
        val request = SearchRequest(0, 0, query = MatchQuery("properties.nst", "*"), fields = listOf(Field("properties.nst")))
        val result = syncDao.search(request)
        assertEquals(19, result.totalItems)
    }

    @Test
    fun testNotStarMatchQuery() {
        val request = SearchRequest(0, 0, query = NotQuery(MatchQuery("properties.nst", "*")), fields = listOf(Field("properties.nst")))
        val result = syncDao.search(request)
        assertEquals(27, result.totalItems)
    }

    @Test
    fun testExists() {
        val request = SearchRequest(0, 0, ExistsQuery("properties.alert"))
        val result = syncDao.search(request)
        assertEquals(3, result.totalItems)
    }

}
