package io.em2m.search.core.xform

import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.Transformer
import io.em2m.simplex.model.ExprContext

class LuceneTransformer<T> : Transformer<T> {
    override fun transformRequest(request: SearchRequest, context: ExprContext): SearchRequest {
        val query = request.query?.let { LuceneQueryTransformer().transform(it, context) }
        return request.copy(query = query)
    }
}
