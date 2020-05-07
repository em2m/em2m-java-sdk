package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class NamedTransformer<T>(val namedAggs: Map<String, Agg>) : Transformer<T> {

    // TODO - Timezone needs to propogate down from the request
    private val queryXform = NamedAggQueryTransformer(namedAggs, "America/Los_Angeles")
    private val aggXform = NamedAggTransformer(namedAggs)

    override fun transformRequest(request: SearchRequest): SearchRequest {
        val query = request.query?.let { queryXform.transform(it) }
        val aggs = request.aggs.map { aggXform.transform(it) }
        return request.copy(query = query, aggs = aggs)
    }

    override fun transformResult(request: SearchRequest, result: SearchResult<T>): SearchResult<T> {
        val aggMap = request.aggs.associateBy { it.key }
        val aggs = result.aggs.mapValues { (key, aggResult) ->
            val reqAgg = aggMap[key]
            if (reqAgg is NamedAgg) {
                val buckets = aggResult.buckets?.map { it.copy(query = NamedQuery(name = reqAgg.name, value = it.key)) }
                AggResult(key = key, buckets = buckets, stats = aggResult.stats, value = aggResult.value, op = "filters")
            } else aggResult
        }
        return result.copy(aggs = aggs)
    }

}