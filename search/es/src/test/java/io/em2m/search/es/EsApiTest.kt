package io.em2m.search.es

import org.junit.Assert
import org.junit.Test
import org.locationtech.jts.geom.Envelope


class EsApiTest : FeatureTestBase() {

    val type = FeatureTestBase.type
    val index = FeatureTestBase.index

    @Test
    fun testMatchAll() {
        val request = EsSearchRequest(from = 0, size = 20, query = EsMatchAllQuery())
        val result = esClient.search(index, type, request)
        Assert.assertEquals(46, result.hits.total)
        Assert.assertEquals(20, result.hits.hits.size)
    }

    @Test
    fun testSort() {
        val request = EsSearchRequest(from = 0, size = 10, query = EsMatchAllQuery(), sort = listOf(mapOf("properties.mag" to "asc")))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(2.5, result.hits.hits[0].source?.with("properties")?.get("mag")?.asDouble())
    }

    @Test
    fun testQueryString() {
        val request = EsSearchRequest(from = 0, size = 5, query = EsQueryStringQuery("properties.mag:[4.0 TO *]"))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(16, result.hits.total)
        Assert.assertEquals(5, result.hits.hits.size)
    }

    @Test
    fun testRange() {
        val request = EsSearchRequest(from = 0, size = 5, query = EsRangeQuery("properties.mag", gte = 4))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(16, result.hits.total)
        Assert.assertEquals(5, result.hits.hits.size)
    }

    @Test
    fun testType() {
        val request = EsSearchRequest(from = 0, size = 0, query = EsTypeQuery(type))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(46, result.hits.total)
    }

    @Test
    fun testTerm() {
        val request = EsSearchRequest(from = 0, size = 0, query = EsTermQuery("properties.mag", value = "2.5"))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(3, result.hits.total)
    }

    @Test
    fun testPrefix() {
        val request = EsSearchRequest(from = 0, size = 5, query = EsPrefixQuery("id", value = "nn"))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(13, result.hits.total)
    }

    @Test
    fun testBool() {
        val request = EsSearchRequest(from = 0, size = 5, query = EsBoolQuery(
                should = listOf(EsQueryStringQuery("properties.mag:2.5"), EsQueryStringQuery("properties.mag:5.2"))))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(4, result.hits.total)
        Assert.assertEquals(4, result.hits.hits.size)
    }

    @Test
    fun testGeoBoundingBox() {
        val request = EsSearchRequest(from = 0, size = 5, query = EsGeoBoundingBoxQuery("geometry.coordinates", Envelope(-180.0, 180.0, -90.0, 90.0)))
        val result = esClient.search(index, type, request)
        Assert.assertEquals(46, result.hits.total)
    }

    @Test
    fun testFields() {
        val request = EsSearchRequest(from = 0, size = 10, query = EsMatchAllQuery(), source = listOf("id", "properties.time"))
        val result = esClient.search(index, type, request)

        Assert.assertEquals(46, result.hits.total)
        Assert.assertEquals(10, result.hits.hits.size)
        val esHit: EsHit = result.hits.hits[0]
        Assert.assertNotNull(esHit.source?.get("id"))
        Assert.assertNotNull(esHit.source?.get("properties")?.get("time"))
    }

    @Test
    fun testAggs() {
        val aggs = EsAggs()
        aggs.term("magnitudeType", "properties.magnitudeType", 10, EsSortType.COUNT, EsSortDirection.ASC)
        //aggs.stats("magStats", "properties.mag", SortType.COUNT, SortDirection.ASC)
        val request = EsSearchRequest(from = 0, size = 0, aggs = aggs)
        val result = esClient.search(index, type, request)
        Assert.assertEquals(46, result.hits.total)
        Assert.assertEquals(3, result.aggregations["magnitudeType"]?.buckets?.size)
        //assertEquals(46, result.aggregations["magStats"]?.count)
        //assertEquals(2.5, result.aggregations["magStats"]?.min)
        //assertEquals(5.2, result.aggregations["magStats"]?.max)
    }

    @Test
    fun testGetIndices() {

    }

}
