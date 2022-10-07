package io.em2m.search.bean

import io.em2m.search.core.deeppaging.DeepPagingItemIterable
import io.em2m.search.core.model.*
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals

class MapBackedDaoDeepPagingTest {
    private var dao: MapBackedSyncDao<Movie> by Delegates.notNull()
    private var deepPagingMovieIterable: DeepPagingItemIterable<Movie> by Delegates.notNull()
    private var emptyDeepPagingItemIterable: DeepPagingItemIterable<Any> by Delegates.notNull()

    companion object {
        private var movies = Movie.load()
    }

    @Before
    fun setup() {
        dao = MapBackedSyncDao(MovieMapper(), movies)
        deepPagingMovieIterable = DeepPagingItemIterable(
            searchable = dao,
            sorts = listOf(DocSort("id", Direction.Descending)),
            idField = "id",
            chunkSize = 1000
        )
        emptyDeepPagingItemIterable = DeepPagingItemIterable(
            {  SearchResult(totalItems = 0) },
            sorts = listOf(DocSort("id", Direction.Descending)),
            idField = "id",
            chunkSize = 1000
        )
    }

    @Test
    fun `count function returns correct size`() {
        val moviesCount = deepPagingMovieIterable.count()
        assertEquals(5000, moviesCount)
    }

    @Test(expected = NoSuchElementException::class)
    fun `throws exception for searchable with no results`() {
        emptyDeepPagingItemIterable.iterator().next()
    }

    @Test(expected = NoSuchElementException::class)
    fun `throws exception when fetching more items fails unexpectedly`() {
        var numberOfSearches = 0
        val searchable: Searchable<Movie> = Searchable {
            val result = when (numberOfSearches) {
                0 -> dao.search(it)
                else -> SearchResult(totalItems = 0)
            }
            numberOfSearches++
            result
        }
        val brokenDeepPagingItemIterator = DeepPagingItemIterable<Movie>(
            searchable = searchable,
            sorts = listOf(DocSort("id", Direction.Descending)),
            idField = "id",
            chunkSize = 1000
        ).iterator()
        while (brokenDeepPagingItemIterator.hasNext()) {
            brokenDeepPagingItemIterator.next()
        }
    }

    @Test
    fun `iterates through all results`() {
        val idsList = deepPagingMovieIterable.map { it.id }
        assertEquals(5000, idsList.size)
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
