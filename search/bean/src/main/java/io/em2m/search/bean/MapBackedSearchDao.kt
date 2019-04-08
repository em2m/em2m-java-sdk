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

import io.em2m.search.core.daos.AbstractSearchDao
import io.em2m.search.core.model.*
import io.em2m.search.core.xform.SimplifyQueryTransformer
import rx.Observable
import rx.Observable.just
import java.util.*

class MapBackedSearchDao<T>(idMapper: IdMapper<T>, val items: MutableMap<String, T> = HashMap()) : AbstractSearchDao<T>(idMapper) {

    override fun create(entity: T): Observable<T> {
        throw NotImplementedError()
    }

    override fun deleteById(id: String): Observable<Boolean> {
        return if (items.containsKey(id)) {
            Observable.error(NotFoundException())
        } else {
            Observable.just(items.remove(id) != null)
        }
    }

    override fun search(request: SearchRequest): Observable<SearchResult<T>> {
        val matches = if (request.sorts.isNotEmpty()) {
            findMatches(request.query).sortedWith(MapBackedSyncDao.CompositeComparator(request.sorts))
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
        return Observable.just(SearchResult(aggResults, items, rows, totalItems.toLong()))
    }

    override fun findById(id: String): Observable<T?> {
        return Observable.just(items[id])
    }

    override fun save(id: String, entity: T): Observable<T> {
        items.put(id, entity)
        // TODO: Need a function for setting the ID on the entity
        return just(entity)
    }

    private fun findMatches(query: Query?): List<T> {
        val predicate: (Any) -> Boolean = Functions.toPredicate(simplifyQuery(query))
        return items.values.filter { predicate.invoke(it as Any) }
    }

    private fun simplifyQuery(query: Query?): Query {
        return if (query != null) {
            SimplifyQueryTransformer().transform(query)
        } else MatchAllQuery()
    }

    fun buildRows(matches: List<T>, fields: List<Field>): List<List<Any?>> {
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


}
