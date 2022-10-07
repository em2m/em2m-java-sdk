package io.em2m.search.bean

import io.em2m.search.core.deeppaging.DeepPagingItemIterable
import io.em2m.search.core.model.*
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals

class MapBackedDaoDeepPagingTest {
    private var dao: MapBackedSyncDao<Movie> by Delegates.notNull()
    private var deepPagingItemIterable: DeepPagingItemIterable<Movie> by Delegates.notNull()

    companion object {
        private var movies = Movie.load()
    }

    @Before
    fun setup() {
        dao = MapBackedSyncDao(MovieMapper(), movies)
        deepPagingItemIterable = DeepPagingItemIterable(
            searchable = dao,
            sorts = listOf(DocSort("id", Direction.Descending)),
            idField = "id",
            chunkSize = 1000
        )
    }

    @Test
    fun `count function returns correct size`() {
        val moviesCount = deepPagingItemIterable.count()
        assertEquals(5000, moviesCount)
    }

    class MovieMapper : IdMapper<Movie> {

        override val idField: String = "id"

        override fun setId(obj: Movie, id: String): Movie {
            return obj.copy(id = id)
        }

        override fun getId(obj: Movie): String {
            return obj.id
        }

    }
}
