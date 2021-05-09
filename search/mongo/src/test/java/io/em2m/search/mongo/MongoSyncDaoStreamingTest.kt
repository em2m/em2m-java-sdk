package io.em2m.search.mongo

import io.em2m.search.core.model.Field
import org.junit.Test

class MongoSyncDaoStreamingTest : FeaturesTestBase() {

    @Test
    fun `stream all items`() {
        val result = syncDao.streamItems().asSequence().toList()
        assertEquals(46, result.size)
    }

    @Test
    fun `stream all rows`() {
        val result = syncDao.streamRows(fields = listOf(Field("properties.mag"))).asSequence().toList()
        assertEquals(46, result.size)
    }

}
