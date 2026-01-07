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

import io.em2m.search.core.daos.AbstractSyncDao
import io.em2m.search.core.model.*
import io.em2m.utils.OperationType
import java.lang.Exception
import kotlin.reflect.KClass

class MapBackedSyncDao<T>(idMapper: IdMapper<T>, private val items: MutableMap<String, T> = HashMap()) : AbstractSyncDao<T>(idMapper), StreamableDao<T> {

    override fun create(entity: T): T? {
        return save(idMapper.getId(entity), entity)
    }

    override fun deleteById(id: String): Boolean {
        return if (items.containsKey(id)) {
            items.remove(id) != null
        } else {
            throw NotFoundException()
        }
    }

    fun deleteByQuery(query: Query): Boolean {
        val predicate: (Any) -> Boolean = Functions.toPredicate(query?.simplify() ?: MatchAllQuery())
        val removeKeys = items.entries
            .filter { (_, value) -> value is Any && predicate(value) }
            .map(Map.Entry<String, T>::key)
        return removeKeys.map {
            try {
                deleteById(it)
            } catch (_ : Exception) {
                false
            }
        }.all { it }
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val matches = if (request.sorts.isNotEmpty()) {
            findMatches(request.query).sortedWith(CompositeComparator(request.sorts))
        } else {
            findMatches(request.query)
        }

        val aggResults = Aggs.processAggs(request.aggs, matches)
        val totalItems = matches.size

        var items: List<T>? = null
        var rows: List<List<Any?>>? = null

        if (request.fields.isEmpty()) {
            items = matches.page(request.offset.toInt(), request.limit.toInt())
        } else {
            rows = buildRows(matches.page(request.offset.toInt(), request.limit.toInt()), request.fields)
        }
        return SearchResult(aggResults, items, rows, totalItems.toLong(), fields = request.fields)
    }

    override fun findById(id: String): T? {
        return items[id]
    }

    override fun save(id: String, entity: T): T? {
        items[id] = entity
        // TODO: Need a function for setting the ID on the entity
        return entity
    }

    override fun upsert(id: String, entity: T): T? {
        return this.items.putIfAbsent(id, entity)
    }

    override fun upsertBatch(entities: List<T>): List<T> {
        return entities.mapNotNull { this.items.putIfAbsent(idMapper(it), it) }
    }

    private fun findMatches(query: Query?): List<T> {
        val predicate: (Any) -> Boolean = Functions.toPredicate(query?.simplify() ?: MatchAllQuery())
        return items.values.filter { predicate.invoke(it as Any) }
    }

    private fun buildRows(matches: List<T>, fields: List<Field>): List<List<Any?>> {
        val getters: List<(Any) -> Any?> = fields
                .map { it.name }
                .map { name ->
                    if (name != null) {
                        Functions.fieldValue(name)
                    } else {
                        { null }
                    }
                }
        return matches.map { item ->
            getters.map { if (item == null) null else it.invoke(item as Any) }
        }
    }

    override fun streamRows(
        fields: List<Field>,
        query: Query,
        sorts: List<DocSort>,
        params: Map<String, Any>
    ): Iterator<List<Any?>> {
        val request = SearchRequest(fields = fields, query = query, sorts = sorts, params = params, limit = 1_000_0000)
        return (search(request).rows ?: emptyList()).iterator()
    }

    override fun streamItems(query: Query, sorts: List<DocSort>, params: Map<String, Any>): Iterator<T> {
        val request = SearchRequest(query = query, sorts = sorts, params = params, limit = 1_000_0000)
        return (search(request).items ?: emptyList()).iterator()
    }

    class CompositeComparator<T>(sorts: List<DocSort>) : Comparator<T> {

        private val comparators = sorts.map {
            val c = SortComparator<T>(it)
            if (it.direction == Direction.Descending) {
                c.reversed()
            } else {
                c
            }
        }

        override fun compare(o1: T, o2: T): Int {
            comparators.forEach { c ->
                val value = c.compare(o1, o2)
                if (value != 0) return value
            }
            return 0
        }
    }

    class SortComparator<T>(sort: DocSort) : Comparator<T> {

        private val expr = Functions.fieldValue(sort.field)

        override fun compare(o1: T, o2: T): Int {
            val v1 = if (o1 != null) expr.invoke(o1 as Any) else null
            val v2 = if (o2 != null) expr.invoke(o2 as Any) else null
            return if (v1 is Number && (v2 is Number)) {
                compareNumbers(v1, v2)
            } else {
                v1.toString().compareTo(v2.toString())
            }
        }

        private fun compareNumbers(n1: Number?, n2: Number?): Int {
            return when {
                (n1 == n2) -> 0
                (n2 == null) -> 1
                (n1 == null) -> -1
                (n1 is Float) -> n1.compareTo(n2.toFloat())
                (n1 is Double) -> n1.compareTo(n2.toDouble())
                (n1 is Int) -> n1.compareTo(n2.toInt())
                (n1 is Long) -> n1.compareTo(n2.toLong())
                (n1 is Short) -> n1.compareTo(n2.toShort())
                else -> n1.toDouble().compareTo(n2.toDouble())
            }
        }
    }

    override fun getPriority(type: OperationType): Int {
        // if caching in a map, creates and deletes should be done last compared to actual values
        return when (type) {
            OperationType.CREATE -> OperationType.LOW_PRIORITY
            OperationType.READ ->   OperationType.MEDIUM_PRIORITY
            OperationType.SEARCH -> OperationType.MEDIUM_PRIORITY
            OperationType.UPDATE -> OperationType.LOW_PRIORITY
            OperationType.DELETE -> OperationType.LOW_PRIORITY
            else -> super<StreamableDao>.getPriority(type)
        }
    }

    override fun toString(): String {
        val maybeT: T? = items.values.firstOrNull() as? T
        var clazz: KClass<Any>? = null
        if (maybeT != null) {
            val t: T = maybeT
            clazz = t!!::class as? KClass<Any>
        }
        val classString = if (clazz == null) { "" } else { "class=${clazz.simpleName} "}
        return "${javaClass.name} { size = ${items.size} $classString }"
    }

}
