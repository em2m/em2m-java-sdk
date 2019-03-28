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

import com.scaleset.utils.Coerce
import io.em2m.search.core.model.*
import io.em2m.simplex.evalPath
import java.lang.reflect.Array
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


fun <T> List<T>.page(offset: Int, limit: Int): List<T> {
    val end = Math.min(this.size, offset + limit) - 1
    return if (this.size < offset) {
        emptyList()
    } else {
        this.slice(offset..end)
    }
}

class Functions {

    companion object {

        private fun term(path: String, term: Any?): (Any) -> Boolean {
            val fieldGetter = field(path)
            return { obj ->
                val values = fieldGetter.invoke(obj)
                var result = false
                for (value in values) {
                    if (value is Number) {
                        val termVal = Coerce.to(term, value.javaClass)
                        result = termVal == value
                    } else if (value != null) {
                        val termStr = term.toString()
                        result = value == termStr
                    }
                    if (result) break
                }
                result
            }
        }

        private fun all(predicates: List<(Any) -> Boolean>): (Any) -> Boolean {
            return { obj ->
                var result = true
                for (p in predicates) {
                    if (!p(obj)) {
                        result = false
                        break
                    }
                }
                result
            }
        }

        private fun any(predicates: List<(Any) -> Boolean>): (Any) -> Boolean {
            return { obj ->
                var result = false
                for (p in predicates) {
                    if (p(obj)) {
                        result = true
                        break
                    }
                }
                result
            }
        }

        private fun matches(path: String, pattern: Pattern): (Any) -> Boolean {
            val fieldGetter = field(path)
            return { obj ->
                var result = false
                val values = fieldGetter.invoke(obj)
                for (value in values) {
                    if (value != null) {
                        result = pattern.matcher(Coerce.toString(value)).matches()
                    }
                    if (result) break
                }
                result
            }
        }

        private fun not(predicate: (Any) -> Boolean): (Any) -> Boolean {
            return { obj -> !predicate(obj) }
        }

        private fun asList(obj: Any?): List<Any?> {
            return when {
                obj == null -> emptyList()
                obj is List<*> -> obj.map { it }
                obj.javaClass.isArray -> {
                    val length = Array.getLength(obj)
                    val result = ArrayList<Any>(length)
                    for (i in 0 until length) {
                        val item = Array.get(obj, i)
                        result.add(item)
                    }
                    result
                }
                else -> Arrays.asList(obj)
            }
        }

        private fun lte(path: String, term: Any): (Any) -> Boolean {
            val fieldGetter = field(path)
            return { obj ->
                var result = false
                val values = fieldGetter.invoke(obj)
                for (value in values) {
                    result = compareTo(value, term) <= 0
                    if (result) break
                }
                result
            }
        }

        private fun gte(path: String, term: Any): (Any) -> Boolean {
            val fieldGetter = field(path)
            return { obj ->
                var result = false
                val values = fieldGetter.invoke(obj)
                for (value in values) {
                    result = compareTo(value, term) >= 0
                    if (result) break
                }
                result
            }
        }

        private fun gt(path: String, term: Any): (Any) -> Boolean {
            val fieldGetter = field(path)
            return { obj ->
                var result = false
                val values = fieldGetter.invoke(obj)
                for (value in values) {
                    result = compareTo(value, term) > 0
                    if (result) break
                }
                result
            }
        }

        private fun lt(path: String, term: Any): (Any) -> Boolean {
            val fieldGetter = field(path)
            return { obj ->
                var result = false
                val values = fieldGetter.invoke(obj)
                for (value in values) {
                    result = compareTo(value, term) < 0
                    if (result) break
                }
                result
            }
        }

        fun field(field: String): (Any) -> List<Any?> {
            return { obj ->
                try {
                    asList(obj.evalPath(field))
                } catch (e: Throwable) {
                    emptyList<Any>()
                }
            }
        }

        fun fieldValue(field: String): (Any) -> Any? {
            return { obj ->
                try {
                    obj.evalPath(field)
                } catch (e: Throwable) {
                    null
                }
            }
        }

        private fun toPredicate(terms: List<Query>): List<(Any) -> Boolean> {
            return terms.map { toPredicate(it) }
        }

        private fun toPredicate(expr: PhraseQuery): (Any) -> Boolean {
            val field = expr.field
            val value = expr.value.joinToString(" ")
            val pattern = Pattern.compile(Matcher.quoteReplacement(value), Pattern.CASE_INSENSITIVE)
            return matches(field, pattern)
        }

        private fun toPredicate(expr: PrefixQuery): (Any) -> Boolean {
            val field = expr.field
            val text = expr.value
            val pattern = Pattern.compile("^" + Matcher.quoteReplacement(text) + ".*", Pattern.CASE_INSENSITIVE)
            return matches(field, pattern)
        }

        private fun toPredicate(expr: RegexQuery): (Any) -> Boolean {
            val field = expr.field
            val regex = expr.value
            return matches(field, Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
        }

        internal fun toPredicate(query: RangeQuery): (Any) -> Boolean {
            val field = query.field
            val expr = ArrayList<(Any) -> Boolean>()
            query.lt?.let { expr.add(lt(field, it)) }
            query.lte?.let { expr.add(lte(field, it)) }
            query.gt?.let { expr.add(gt(field, it)) }
            query.gte?.let { expr.add(gte(field, it)) }
            return all(expr)
        }

        private fun toPredicate(expr: TermQuery): (Any) -> Boolean {
            val field = expr.field
            val value = expr.value
            return term(field, value)
        }

        private fun toPredicate(expr: MatchAllQuery): (Any) -> Boolean {
            return { true }
        }

        fun toPredicate(query: Query): (Any) -> Boolean {

            return when (query) {
                is AndQuery -> {
                    all(toPredicate(query.of))
                }
                is OrQuery -> {
                    any(toPredicate(query.of))
                }
                is NotQuery -> {
                    not(any(toPredicate(query.of)))
                }
                is PhraseQuery -> {
                    toPredicate(query)
                }
                is RegexQuery -> {
                    toPredicate(query)
                }
                is PrefixQuery -> {
                    toPredicate(query)
                }
                is RangeQuery -> {
                    toPredicate(query)
                }
                is TermQuery -> {
                    toPredicate(query)
                }
                is MatchAllQuery -> {
                    toPredicate(query)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported expression type")
                }
            }
        }

        fun compareTo(first: Any?, second: Any?): Int {
            return if (first == null) {
                return -1
            } else (first as? String)?.compareTo(Coerce.toString(second))
                    ?: (first as? Int)?.compareTo(Coerce.toInteger(second))
                    ?: (first as? Long)?.compareTo(Coerce.toLong(second))
                    ?: (first as? Float)?.compareTo(Coerce.toDouble(second)?.toFloat() ?: 0F)
                    ?: (first as? Double)?.compareTo(Coerce.toDouble(second))
                    ?: first.toString().compareTo(second.toString())
        }

    }
}
