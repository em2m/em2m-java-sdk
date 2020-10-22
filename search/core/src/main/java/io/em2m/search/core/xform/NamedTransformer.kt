package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class NamedTransformer<T>(private val namedAggs: Map<String, Agg>) : Transformer<T> {

    // TODO - Timezone needs to propogate down from the request
    private val queryXform = NamedAggQueryTransformer(namedAggs, "America/Los_Angeles")
    private val aggXform = NamedAggTransformer(namedAggs)

    override fun transformQuery(query: Query?): Query? {
        return query?.let { queryXform.transform(it) }
    }

    override fun transformRequest(request: SearchRequest): SearchRequest {
        val tz = request.params["timeZone"]?.toString() ?: "America/Los_Angeles"
        val query = request.query?.let { NamedAggQueryTransformer(namedAggs, tz).transform(it) } ?: request.query
        val aggs = request.aggs.map { aggXform.transform(it) }
        return request.copy(query = query, aggs = aggs)
    }

    override fun transformResult(request: SearchRequest, result: SearchResult<T>): SearchResult<T> {
        val aggMap = request.aggs.associateBy { it.key }
        val aggs = result.aggs.mapValues { (key, aggResult) ->
            val reqAgg = aggMap[key]
            if (reqAgg is NamedAgg) {
                val buckets = aggResult.buckets?.map { it.copy(query = NamedQuery(name = reqAgg.name, value = it.key)) }
                val field: String? = field(reqAgg)
                val type: String? = type(reqAgg)
                AggResult(key = key, buckets = buckets, stats = aggResult.stats, value = aggResult.value, op = "filters", field = field, type = type)
            } else aggResult
        }
        return result.copy(aggs = aggs)
    }

    fun field(namedAgg: NamedAgg): String? {
        val agg = namedAggs[namedAgg.name]
        return when (agg) {
            is DateRangeAgg -> agg.field
            is DateHistogramAgg -> agg.field
            else -> null
        }
    }

    fun type(namedAgg: NamedAgg): String? {
        val agg = namedAggs[namedAgg.name]
        return when (agg) {
            is DateRangeAgg -> "date"
            is DateHistogramAgg -> "date"
            else -> null
        }
    }
}