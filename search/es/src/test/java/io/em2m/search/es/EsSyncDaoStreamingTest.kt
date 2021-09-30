package io.em2m.search.es

import io.em2m.geo.feature.Feature
import io.em2m.search.core.model.Field
import io.em2m.search.core.model.MatchAllQuery
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.StreamableDao
import io.em2m.search.core.xform.DeepPagingTransformer
import io.em2m.search.core.xform.TransformingDao
import io.em2m.utils.coerce
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates

class EsSyncDaoStreamingTest : FeatureTestBase() {

    private var searchDao: StreamableDao<Feature> by Delegates.notNull()

    @Before
    override fun before() {
        super.before()
        searchDao = EsSyncDao(esClient, index, type, Feature::class.java, idMapper)
    }

    @Test
    fun `stream all items`() {
        val result = searchDao.streamItems().asSequence().toList()
        assertEquals(46, result.size)
    }

    @Test
    fun `stream all rows`() {
        val result = searchDao.streamRows(fields = listOf(Field("properties.mag"))).asSequence().toList()
        assertEquals(46, result.size)
    }

    @Test
    fun `deep paging transform`() {
        val pagingDao = TransformingDao(DeepPagingTransformer("id"), searchDao)
        val rows = ArrayList<List<Any?>>()
        val params = mutableMapOf<String, Any>("deepPage" to true)
        do {
            val request = SearchRequest(
                limit = 10,
                query = MatchAllQuery(),
                fields = listOf(Field("properties.mag")),
                params = params
            )
            val result = pagingDao.search(request)
            rows.addAll(result.rows ?: emptyList())
            val lastKey = result.headers["lastKey"]
            if (lastKey != null) {
                params["lastKey"] = lastKey
            }
        } while (result.totalItems > (result.rows?.size ?: 0))
        assertEquals(46, rows.size)
    }

}
