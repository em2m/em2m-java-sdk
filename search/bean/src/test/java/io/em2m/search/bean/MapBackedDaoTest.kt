/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________

 * Copyright (c) 2013-2016 Elastic M2M Incorporated, All Rights Reserved.

 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated

 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.search.bean


import io.em2m.search.core.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates.notNull

class MapBackedDaoTest {
    private var dao: MapBackedSyncDao<Movie> by notNull()

    @Before
    fun setup() {
        dao = MapBackedSyncDao(MovieMapper(), movies)
    }

    @Test
    @Throws(Exception::class)
    fun testFindOne() {
        val movie = dao.findOne(TermQuery("fields.title", "RoboCop")) ?: error("expect movie")
        assertTrue(dao.exists(movie.id))
    }

    @Test
    @Throws(Exception::class)
    fun testSearch() {
        val req = SearchRequest(limit = 10, query = TermQuery("fields.genres", "Sci-Fi"))
        val results = dao.search(req)
        assertNotNull(results)
        assertNotNull(results.items)
        assertEquals(591, results.totalItems)
        assertEquals(10, results.items?.size)
    }

    @Test
    @Throws(Exception::class)
    fun testFields() {
        val req = SearchRequest(
                limit = 10,
                fields = listOf(Field("fields.title"), Field("fields.genres")))
        val results = dao.search(req)
        assertNotNull(results)
        assertNotNull(results.rows)
        assertNull(results.items)
        assertEquals(5000, results.totalItems)
        assertEquals(10, results.rows?.size)
        val row = requireNotNull(results.rows?.get(0))
        assertEquals(2, row.size)
        assertTrue(row[0] is String)
    }

    @Test
    @Throws(Exception::class)
    fun testAgg() {
        val req = SearchRequest(limit = 0, query = MatchAllQuery(), aggs = listOf(
                TermsAgg("fields.actors", key = "actors", size = 10),
                FiltersAgg(mapOf("sci-fi" to TermQuery("fields.genres", "Sci-Fi"), "fantasy" to TermQuery("fields.genres", "Drama")), key = "genres")))
        val results = dao.search(req)
        assertNotNull(results)
        assertNotNull(results.items)
        assertEquals(5000, results.totalItems)
        assertEquals(0, results.items?.size)
        assertEquals(2, results.aggs.size)
    }

    @Test
    @Throws(Exception::class)
    fun testSort() {
        val req = SearchRequest(limit = 100, fields = listOf(Field(name = "fields.rank")),
                query = MatchAllQuery(), sorts = listOf(DocSort("fields.rank", Direction.Descending))
        )
        val results = dao.search(req)
        assertNotNull(results)
        assertNotNull(results.rows)
        assertEquals(5000, results.rows?.get(0)?.get(0))
    }

    companion object {
        private var movies = Movie.load()
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
