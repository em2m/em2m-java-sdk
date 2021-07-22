package io.em2m.search.es

import io.em2m.geo.feature.Feature
import io.em2m.search.core.model.Field
import io.em2m.search.core.model.StreamableDao
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

}
