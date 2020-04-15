package io.em2m.search.core.xform

import io.em2m.search.core.daos.SyncDaoWrapper
import io.em2m.search.core.model.*

class TransformingDao<T>(private val transformer: Transformer<T>, delegate: SyncDao<T>) : SyncDaoWrapper<T>(delegate) {

    override fun search(request: SearchRequest): SearchResult<T> {
        val req = transformer.transformRequest(request)
        val result = delegate.search(req)
        return transformer.transformResult(request, result).transformItems { transformer.transformItem(it) }
    }

    override fun count(query: Query): Long {
        val q = transformer.transformQuery(query) ?: MatchAllQuery()
        return super.count(q ?: MatchAllQuery())
    }

    override fun findOne(query: Query): T? {
        val q = transformer.transformQuery(query) ?: MatchAllQuery()
        val item = super.findOne(q)
        return item?.let { transformer.transformItem(item) }
    }

}