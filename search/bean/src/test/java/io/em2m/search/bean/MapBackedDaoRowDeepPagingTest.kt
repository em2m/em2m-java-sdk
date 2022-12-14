package io.em2m.search.bean

import io.em2m.search.core.deeppaging.DeepPagingRowIterable
import io.em2m.search.core.model.Field
import io.em2m.search.core.model.IdMapper
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapBackedDaoRowDeepPagingTest {
    private var dao: MapBackedSyncDao<Movie> by Delegates.notNull()
    private var deepPagingMovieIdIterable: DeepPagingRowIterable by Delegates.notNull()

    companion object {
        private var moviesMap = Movie.load()
        private val movies = moviesMap.values
    }

    @Before
    fun setup() {
        dao = MapBackedSyncDao(MovieMapper(), moviesMap)
        deepPagingMovieIdIterable = DeepPagingRowIterable(
            searchable = dao,
            idField = "id",
            chunkSize = 1000,
            fields = listOf(
                Field(name = "id")
            )
        )
    }

    @Test
    fun `count function returns correct size`() {
        val moviesCount = deepPagingMovieIdIterable.count()
        assertEquals(5000, moviesCount)
    }

    @Test
    fun `iterates through all rows`() {
        val idsList = deepPagingMovieIdIterable.mapNotNull { it.first() }
        assertTrue { movies.map { it.id }.containsAll(idsList) }
        assertTrue { idsList.containsAll(movies.map { it.id }) }
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

