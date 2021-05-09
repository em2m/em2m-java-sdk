package io.em2m.search.bean

import io.em2m.search.core.model.Field
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals

class MapBackedSyncDaoStreamingTest() {

    private var dao: MapBackedSyncDao<Movie> by Delegates.notNull()
    private var movies = Movie.load()

    @Before
    fun setup() {
        dao = MapBackedSyncDao(MapBackedDaoTest.MovieMapper(), movies)
    }

    @Test
    fun `stream all items`() {
        val result = dao.streamItems().asSequence().toList()
        assertEquals(5000, result.size)
    }

    @Test
    fun `stream all rows`() {
        val result = dao.streamRows(fields = listOf(Field("fields.title"))).asSequence().toList()
        assertEquals(5000, result.size)
    }

}
