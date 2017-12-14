package io.em2m.search.core.daos

import io.em2m.search.core.model.*
import rx.Observable

abstract class AbstractSearchDao<T>(val idMapper: IdMapper<T>) : SearchDao<T> {

    override fun count(query: Query): Observable<Long> {
        return search(SearchRequest(0, 0, query)).map { it.totalItems }
    }

    override fun exists(id: String): Observable<Boolean> {
        return findById(id).map { it != null }
    }

    override fun findById(id: String): Observable<T?> {
        return findOne(TermQuery(idMapper.idField, id))
    }

    override fun findOne(query: Query): Observable<T?> {
        return search(SearchRequest(0, 1, query).countTotal(false))
                .map { result ->
                    if (result.totalItems > 0) {
                        result.items?.get(0)
                    } else null
                }
    }

    override fun saveBatch(entities: List<T>): Observable<List<T>> {
        return Observable.from(entities).flatMap { save(idMapper.getId(it), it) }.toList()
    }

    override fun close() {
    }

    fun generateId(): String {
        return idMapper.generateId()
    }


}