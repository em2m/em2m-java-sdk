package io.em2m.search.core.proxy

import io.em2m.search.core.model.Query
import io.em2m.search.core.model.SearchDao
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.SearchResult
import rx.Observable

class ProxySearchDao<T> : SearchDao<T> {

    override fun create(entity: T): Observable<T> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteById(id: String): Observable<Boolean> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exists(id: String): Observable<Boolean> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun search(request: SearchRequest): Observable<SearchResult<T>> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun count(query: Query): Observable<Long> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findById(id: String): Observable<T?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findOne(query: Query): Observable<T?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun save(id: String, entity: T): Observable<T> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveBatch(entities: List<T>): Observable<List<T>> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
    }

}