package io.em2m.search.core.xform

import io.em2m.search.core.model.Agg
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.Transformer

class NamedTransformer<T>(namedAggs: Map<String, Agg>) : Transformer<T> {

    // TODO - Timezone needs to propogate down from the request
    private val queryXform = NamedAggQueryTransformer(namedAggs, "America/Los_Angeles")
    private val aggXform = NamedAggTransformer(namedAggs)

    override fun transformRequest(request: SearchRequest): SearchRequest {
        val query = request.query?.let { queryXform.transform(it) }
        val aggs = request.aggs.map { aggXform.transform(it) }
        return request.copy(query = query, aggs = aggs)
    }

}