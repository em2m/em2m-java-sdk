package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.utils.coerce

class XformTransformer<T>() : Transformer<T> {

    override fun transformRequest(request: SearchRequest): SearchRequest {
        val aggs = request.aggs.map { aggXform.transform(it) }
        return request.copy(aggs = aggs)
    }

    override fun transformResult(request: SearchRequest, result: SearchResult<T>): SearchResult<T> {
        val aggs = request.aggs.mapNotNull { agg ->
            if (agg is XformAgg) {
                val aggResult = result.aggs[agg.agg.key]
                if (aggResult != null) transformAggResult(request, agg, aggResult)
                else null
            } else result.aggs[agg.key]
        }
        return result.copy(aggs = aggs.associateBy { it.key })
    }

    private fun transformAggResult(request: SearchRequest, agg: XformAgg, result: AggResult): AggResult {
        val buckets: List<Bucket>? = result.buckets?.mapNotNull { bucket ->
            val fieldValues = mapOf("bucket" to bucket)
            val context = BucketContext(request, emptyMap(), bucket).toMap().plus("fieldValues" to fieldValues)
            agg.bucket?.call(context).coerce<Bucket>()
        }
        return result.copy(key = agg.key, buckets = buckets)
    }

    companion object {

        val aggXform = object : AggTransformer() {

            override fun transformXformAgg(agg: XformAgg): Agg {
                return agg.agg
            }

        }
    }

}