package io.em2m.search.core.xform

import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.Transformer

class LuceneTransformer<T> : Transformer<T> {
    override fun transformRequest(request: SearchRequest): SearchRequest {
        val query = request.query?.let { LuceneQueryTransformer().transform(it) }
        return request.copy(query = query)
    }
}
