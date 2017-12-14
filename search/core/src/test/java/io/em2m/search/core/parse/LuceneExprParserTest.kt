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
package io.em2m.search.core.parse

import io.em2m.search.core.model.*
import io.em2m.search.core.parser.LuceneExprParser
import org.junit.Assert
import org.junit.Test


class LuceneExprParserTest : Assert() {

    @Test
    fun testTermQuery() {
        val exprParser = LuceneExprParser("text")
        val result = exprParser.parse("field:value")
        Assert.assertNotNull(result)
        Assert.assertEquals("value", (result as TermQuery).value)
    }

    @Test
    fun testBooleanTermQuery() {
        val exprParser = LuceneExprParser("text")
        val result = exprParser.parse("field:a AND field:b")
        Assert.assertNotNull(result)
        Assert.assertTrue(result is BoolQuery)
        val terms = (result as BoolQuery).of
        val ta = terms[0] as TermQuery
        val tb = terms[1] as TermQuery
        Assert.assertEquals("a", ta.value)
        Assert.assertEquals("b", tb.value)
    }

    @Test
    fun testRangeQuery() {
        val exprParser = LuceneExprParser("text")
        val result = exprParser.parse("field:{1 TO 5]")
        Assert.assertNotNull(result)
        Assert.assertTrue(result is RangeQuery)
        val expr = result as RangeQuery
        Assert.assertEquals("1", expr.gt)
        Assert.assertEquals("5", expr.lte)
    }

    @Test
    fun testProhibitBoolean() {
        val exprParser = LuceneExprParser("text")
        val result = exprParser.parse("-(Fred Flinstone)")
        Assert.assertNotNull(result)
        Assert.assertTrue(result is BoolQuery)
        val children = (result as BoolQuery).of
        Assert.assertTrue(children[0] is BoolQuery)
        //List<Query> terms = ((BoolQuery) children).getChildren();
        //assertEquals("Fred", ((TermQuery) terms.get(0)).getValue());
    }

    @Test
    fun testPhraseQuery() {
        val exprParser = LuceneExprParser("text")
        val result = exprParser.parse("name:\"Fred Flinstone\"")
        Assert.assertNotNull(result)
        Assert.assertTrue(result is PhraseQuery)
        val expr = result as PhraseQuery
        Assert.assertEquals("name", expr.field)
        Assert.assertEquals(2, expr.value.size.toLong())
        Assert.assertEquals("Fred", expr.value[0])
    }

    @Test
    fun testPrefixQuery() {
        val exprParser = LuceneExprParser("text")
        val result = exprParser.parse("name:Flin*")
        Assert.assertNotNull(result)
        Assert.assertTrue(result is PrefixQuery)
        val expr = result as PrefixQuery
        Assert.assertEquals("name", expr.field)
        Assert.assertEquals("flin", expr.value)
    }
}
