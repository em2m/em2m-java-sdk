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
import io.em2m.search.core.parser.LuceneExprParser
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MovieQueryTest {
    private val schemaMapper = SimpleSchemaMapper("text")

    @Test
    fun testTitleTerm() {
        val results = find(RegexQuery("fields.title", ".*robocop.*"))
        assertEquals(4, results.size.toLong())
    }

    @Test
    fun testTitleWildcard() {
        val results = find(WildcardQuery("fields.title", "robocop*"))
        assertEquals(4, results.size.toLong())
    }

    @Test
    fun testTitleProhibitedTerm() {
        val results = find(AndQuery(RegexQuery("fields.title", ".*zen.*"), NotQuery(TermQuery("fields.title", "Frozen"))))
        // term queries do not match whole words inside strings - this is not lucene :-(.  Do we want to support analyzed fields?
        // assertEquals(7, results.size.toLong())
        assertEquals(9, results.size.toLong())
    }

    @Test
    fun testTitleProhibitedWildcard() {
        val results = find(AndQuery(RegexQuery("fields.title", ".*zen.*"), NotQuery(PrefixQuery("fields.title", "Fro"))))
        // no tokenization on values, so "The Frozen Groun" doesn't match the prefix query
        assertEquals(8, results.size.toLong())
    }

    @Test
    fun testTitleRange() {
        val results = find("fields.title:[1 TO 2}")
        assertEquals(21, results.size.toLong())
    }

    @Test
    fun testTitlePrefix() {
        val results = find("fields.title:Frozen*")
        assertEquals(3, results.size.toLong())
    }

    @Test
    fun testRankTerm() {
        val results = find("fields.rank:18")
        assertEquals(1, results.size.toLong())
        assertEquals("The Great Gatsby", results[0].fields["title"])
    }

    @Test
    fun testRankRange() {
        assertEquals(100, find("fields.rank:[1 TO 100]").size.toLong())
        assertEquals(100, find("fields.rank:[* TO 100]").size.toLong())
        assertEquals(98, find("fields.rank:{1 TO 100}").size.toLong())
        assertEquals(99, find("fields.rank:{* TO 100}").size.toLong())
    }

    @Test
    fun testGenreTerm() {
        assertEquals(591, find("fields.genres:Sci-Fi").size.toLong())
    }

    @Test
    fun testGenreTerms() {
        assertEquals(1394, find(TermsQuery("fields.genres", listOf("Action", "Sci-Fi"))).size.toLong())
    }

    @Test
    fun testGenrePrefix() {
        assertEquals(1595, find("fields.genres:A*").size.toLong())
    }

    fun find(query: String): List<Movie> {
        //LuceneExpressionConverter mapper = new LuceneExpressionConverter(schema);
        val expr = LuceneExprParser("text").parse(query)
        val predicate = Functions.toPredicate(expr)
        assertNotNull("Unable to parse query", predicate)
        return movies.values.filter(predicate)
    }

    fun find(query: Query): List<Movie> {
        //LuceneExpressionConverter mapper = new LuceneExpressionConverter(schema);
        val predicate = Functions.toPredicate(query)
        assertNotNull("Unable to parse query", predicate)
        return movies.values.filter(predicate)
    }

    companion object {
        private var movies: Map<String, Movie> = Movie.load()
    }

}
