package io.em2m.search.bean

import io.em2m.search.core.deeppaging.DeepPagingItemIterable
import io.em2m.search.core.model.*
import io.em2m.simplex.evalPath
import io.em2m.utils.coerce
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapBackedDaoItemDeepPagingTest {
    private var dao: MapBackedSyncDao<Movie> by Delegates.notNull()
    private var deepPagingMovieIterable: DeepPagingItemIterable<Movie> by Delegates.notNull()
    private var sortedMovieIterable: DeepPagingItemIterable<Movie> by Delegates.notNull()
    private var emptyDeepPagingItemIterable: DeepPagingItemIterable<Any> by Delegates.notNull()

    companion object {
        private var movieMap = Movie.load()
        private val movies = movieMap.values
    }

    @Before
    fun setup() {
        dao = MapBackedSyncDao(MovieMapper(), movieMap)
        deepPagingMovieIterable = DeepPagingItemIterable(
            searchable = dao,
            idField = "id",
            chunkSize = 1000
        )
        emptyDeepPagingItemIterable = DeepPagingItemIterable(
            {  SearchResult(totalItems = 0) },
            sorts = listOf(DocSort("id", Direction.Descending)),
            idField = "id",
            chunkSize = 1000
        )
        sortedMovieIterable = DeepPagingItemIterable(
            searchable = dao,
            idField = "id",
            sorts = listOf(DocSort(field = "fields.title", direction = Direction.Descending)),
            chunkSize = 1000
        )
    }

    @Test
    fun `count function returns correct size`() {
        val moviesCount = deepPagingMovieIterable.count()
        assertEquals(5000, moviesCount)
    }

    @Test
    fun `preserves ordering of provided sorts`() {
        var lastMovie: Movie? = null
        sortedMovieIterable.forEach {
            if (lastMovie != null) {
                val lastMovieTitle: String = lastMovie!!.evalPath("fields.title")?.coerce() ?: throw Exception()
                assertTrue {
                    val movieTitle: String = it.evalPath("fields.title")?.coerce() ?: throw Exception()
                    val stillSorted = lastMovieTitle >= movieTitle
                    stillSorted
                }
            }
            lastMovie = it.copy()
        }
    }

    @Test
    fun `gracefully handles searchable with no results`() {
        try {
            emptyDeepPagingItemIterable.iterator().forEach {
                it
            }
            assertTrue { true }
        } catch (e: Exception) {
            // Fail the test if iterable throws exception on empty search result
            assertTrue { false }
        }
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
        val expectedMovieIds = movies.map { it.id }
        assertTrue { expectedMovieIds.containsAll(idsList) }
        assertTrue { idsList.containsAll(expectedMovieIds) }
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
