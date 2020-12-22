package io.em2m.search.core.xform

import io.em2m.search.core.daos.SyncDaoWrapper
import io.em2m.search.core.model.*
import io.em2m.simplex.model.ExprContext

class TransformingDao<T>(private val transformer: Transformer<T>, delegate: SyncDao<T>) : SyncDaoWrapper<T>(delegate) {

    fun search(request: SearchRequest, context: ExprContext): SearchResult<T> {
        val req = transformer.transformRequest(request, context)
        val result = delegate.search(req)
        return transformer.transformResult(request, result, context).transformItems { transformer.transformItem(it, context) }
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        return search(request, emptyMap())
    }

    override fun count(query: Query): Long {
        val q = transformer.transformQuery(query, emptyMap()) ?: MatchAllQuery()
        return super.count(q)
    }

    override fun findOne(query: Query): T? {
        val q = transformer.transformQuery(query, emptyMap()) ?: MatchAllQuery()
        val item = super.findOne(q)
        return item?.let { transformer.transformItem(item, emptyMap()) }
    }

}
