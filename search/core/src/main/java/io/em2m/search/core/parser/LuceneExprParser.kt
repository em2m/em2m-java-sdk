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
package io.em2m.search.core.parser

import io.em2m.search.core.model.*
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermRangeQuery
import org.apache.lucene.search.WildcardQuery
import org.apache.lucene.util.BytesRef
import java.util.*
import java.util.regex.Matcher

class LuceneExprParser(private val defaultField: String? = "_all") {

    fun parse(q: String?): Query {
        val result: Query

        if (q == null || "" == q.trim { it <= ' ' }) {
            result = MatchAllQuery()
        } else {
            val luceneQuery = parseConstraint(q)
            result = handleQuery(luceneQuery)
        }
        return result
    }

    fun handleQuery(query: org.apache.lucene.search.Query): Query {
        return if (query is org.apache.lucene.search.TermQuery) {
            handleTermQuery(query)
        } else if (query is BooleanQuery) {
            handleBooleanQuery(query)
        } else if (query is TermRangeQuery) {
            handleRangeQuery(query)
        } else if (query is org.apache.lucene.search.PrefixQuery) {
            handlePrefixQuery(query)
        } else if (query is WildcardQuery) {
            handleWildcardQuery(query)
        } else if (query is org.apache.lucene.search.PhraseQuery) {
            handlePhraseQuery(query)
        } else {
            throw IllegalArgumentException("Unsupported query type")
        }
    }

    fun handlePhraseQuery(phraseQuery: org.apache.lucene.search.PhraseQuery): PhraseQuery {
        val terms = phraseQuery.terms
        val field = phraseQuery.terms[0].field()
        val phrases = ArrayList<String>()
        for (term in terms) {
            phrases.add(term.text())
        }
        return PhraseQuery(field, phrases)
    }

    fun handleWildcardQuery(wildcardQuery: WildcardQuery): Query {
        val term = wildcardQuery.term
        var value = term.text().replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
        value = Matcher.quoteReplacement(value)
        value = value.replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
        val regex = Matcher.quoteReplacement(value)
        return RegexQuery(term.field(), regex)
    }

    fun handlePrefixQuery(prefixQuery: org.apache.lucene.search.PrefixQuery): Query {
        val term = prefixQuery.prefix
        return PrefixQuery(term.field(), term.text())
    }

    internal fun toString(bytesRef: BytesRef?): String? {
        if (bytesRef != null) {
            return bytesRef.utf8ToString()
        } else {
            return null
        }
    }


    fun handleRangeQuery(rangeQuery: TermRangeQuery): Query {
        val field = rangeQuery.field
        val lower = rangeQuery.lowerTerm?.let { toString(it) }
        val upper = rangeQuery.upperTerm?.let { toString(it) }
        val includesUpper = rangeQuery.includesUpper()
        val includesLower = rangeQuery.includesLower()
        val gt = if (lower != null && !includesLower) lower else null
        val gte = if (lower != null && includesLower) lower else null
        val lt = if (upper != null && !includesUpper) upper else null
        val lte = if (upper != null && includesUpper) upper else null

        return RangeQuery(field, lt = lt, lte = lte, gt = gt, gte = gte)
    }

    /**
     * TODO: Create a single root NOT is all children are prohibited

     * @param boolQuery
     * *
     * @return
     */
    fun handleBooleanQuery(boolQuery: BooleanQuery): Query {
        // De Morgan's Law:
        // "not (A and B)" is the same as "(not A) or (not B)"
        // "not (A or B)" is the same as "(not A) and (not B)".
        //
        val clauses = boolQuery.clauses()
        val nClauses = clauses.size
        val children = ArrayList<Query>()

        var orCount = 0
        var andCount = 0
        var notCount = 0
        for (i in 0..nClauses - 1) {
            val clause = clauses[i]
            if (clause.isRequired) {
                ++andCount
            } else if (clause.isProhibited) {
                ++notCount
            } else {
                ++orCount
            }
            // inverse if our boolean clause is prohibited as a whole
            val prohibit = clause.isProhibited
            val child = handleQuery(clause.query)
            children.add(if (prohibit) NotQuery(listOf(child)) else child)
        }
        if (andCount > 0 && orCount > 0) {
            throw RuntimeException("Mixed boolean clauses not supported!")
        }
        val conjunction = andCount > 0 || notCount > 0
        // inverse is out boolean clause is prohibited as a whole
        val result: Query
        if (children.size == 1) {
            result = children[0]
        } else {
            result = if (conjunction) AndQuery(children) else OrQuery(children)
        }
        return result
    }

    fun handleTermQuery(termQuery: org.apache.lucene.search.TermQuery): TermQuery {
        val term = termQuery.term
        val field = term.field()
        val text = term.text()
        return TermQuery(field, text)
    }

    fun parseConstraint(q: String): org.apache.lucene.search.Query {

        val analyzer = WhitespaceAnalyzer()
        val parser = QueryParser(defaultField, analyzer)
        parser.defaultOperator = QueryParser.Operator.AND
        parser.allowLeadingWildcard = true
        try {
            val result = parser.parse(q)
            return result
        } catch (e: ParseException) {
            throw RuntimeException(e)
        }

    }

}