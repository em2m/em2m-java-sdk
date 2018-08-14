package io.em2m.search.core.daos

import io.em2m.search.core.model.Query
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.SearchResult
import io.em2m.search.core.model.SyncDao

abstract class SyncDaoWrapper<T>(val delegate: SyncDao<T>) : SyncDao<T> {

    override fun create(entity: T): T? {
        return delegate.create(entity)
    }

    override fun deleteById(id: String): Boolean {
        return delegate.deleteById(id)
    }

    override fun exists(id: String): Boolean {
        return delegate.exists(id)
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        return delegate.search(request)
    }

    override fun count(query: Query): Long {
        return delegate.count(query)
    }

    override fun findById(id: String): T? {
        return delegate.findById(id)
    }

    override fun findOne(query: Query): T? {
        return delegate.findOne(query)
    }

    override fun save(id: String, entity: T): T? {
        return delegate.save(id, entity)
    }

    override fun saveBatch(entities: List<T>): List<T> {
        return delegate.saveBatch(entities)
    }

    override fun close() {
        delegate.close()
    }

}