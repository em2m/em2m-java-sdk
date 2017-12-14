package io.em2m.search.core.daos

import io.em2m.search.core.model.Query
import io.em2m.search.core.model.SearchDao
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.SearchResult
import rx.Observable

abstract class SearchDaoWrapper<T>(val delegate: SearchDao<T>) : SearchDao<T> {

    override fun create(entity: T): Observable<T> {
        return delegate.create(entity)
    }

    override fun deleteById(id: String): Observable<Boolean> {
        return delegate.deleteById(id)
    }

    override fun exists(id: String): Observable<Boolean> {
        return delegate.exists(id)
    }

    override fun search(request: SearchRequest): Observable<SearchResult<T>> {
        return delegate.search(request)
    }

    override fun count(query: Query): Observable<Long> {
        return delegate.count(query)
    }

    override fun findById(id: String): Observable<T?> {
        return delegate.findById(id)
    }

    override fun findOne(query: Query): Observable<T?> {
        return delegate.findOne(query)
    }

    override fun save(id: String, entity: T): Observable<T> {
        return delegate.save(id, entity)
    }

    override fun saveBatch(entities: List<T>): Observable<List<T>> {
        return delegate.saveBatch(entities)
    }

    override fun close() {
        delegate.close()
    }

}