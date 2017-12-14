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

import io.em2m.search.core.parser.LuceneExprParser
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.junit.Assert.*
import org.junit.Test

class QueryConverterTest {

    private val fred = Person("Fred", "Flinstone", 35)
    private val barn = Person("Barney", "Rubble", 27)
    private val schemaMapper = SimpleSchemaMapper("text")

    @Test
    fun testTermQuery() {
        val result = toPredicate("firstName: Fred")
        assertNotNull(result)
        assertTrue(result(fred))
        assertFalse(result(barn))
    }

    @Test
    fun testBooleanAndQuery() {
        val result = toPredicate("firstName:fred AND lastName:flinstone")
        assertNotNull(result)
        assertTrue(result(fred))
        assertFalse(result(barn))
    }

    @Test
    fun testBooleanOrQuery() {
        val result = toPredicate("firstName:(fred OR barney)")
        assertNotNull(result)
        assertTrue(result(fred))
        assertTrue(result(barn))
    }

    internal fun toPredicate(query: String): (Any) -> Boolean {
        val parser = LuceneExprParser("text")
        return Functions.toPredicate(parser.parse(query))
    }

    internal class Person(val firstName: String, val lastName: String, val age: Int)
}
