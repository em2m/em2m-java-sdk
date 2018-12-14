package io.em2m.search.core.daos

import io.em2m.search.core.model.*
import rx.Observable
import rx.Observable.just

class SyncDaoAsyncAdapter<T>(val delegate: SyncDao<T>) : SearchDao<T> {

    override fun create(entity: T): Observable<T> {
        return just(delegate.create(entity))
    }

    override fun deleteById(id: String): Observable<Boolean> {
        return just(delegate.deleteById(id))
    }

    override fun exists(id: String): Observable<Boolean> {
        return just(delegate.exists(id))
    }

    override fun search(request: SearchRequest): Observable<SearchResult<T>> {
        return just(delegate.search(request))
    }

    override fun count(query: Query): Observable<Long> {
        return just(delegate.count(query))
    }

    override fun findById(id: String): Observable<T?> {
        return just(delegate.findById(id))
    }

    override fun findOne(query: Query): Observable<T?> {
        return just(delegate.findOne(query))
    }

    override fun save(id: String, entity: T): Observable<T> {
        return just(delegate.save(id, entity))
    }

    override fun saveBatch(entities: List<T>): Observable<List<T>> {
        return just(delegate.saveBatch(entities))
    }

    override fun close() {
        delegate.close()
    }

}