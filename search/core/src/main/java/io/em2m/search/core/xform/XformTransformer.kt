package io.em2m.search.core.xform

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.search.core.model.*
import io.em2m.simplex.model.Expr
import io.em2m.simplex.model.ExprContext
import io.em2m.utils.coerce

class XformTransformer<T>(val objectMapper: ObjectMapper) : Transformer<T> {

    override fun transformRequest(request: SearchRequest, context: ExprContext): SearchRequest {
        val aggs = request.aggs.map { aggXform.transform(it, context) }
        return request.copy(aggs = aggs)
    }

    override fun transformResult(request: SearchRequest, result: SearchResult<T>, context: ExprContext): SearchResult<T> {
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
        val expr: Expr? = agg.bucket.coerce(objectMapper = objectMapper)

        val buckets: List<Bucket>? = result.buckets?.mapNotNull { bucket ->
            val fieldValues = mapOf("bucket" to bucket)
            val context = BucketContext(request, emptyMap(), bucket).toMap().plus("fieldValues" to fieldValues)
            expr?.call(context).coerce<Bucket>()
        }?.let { buckets ->
            val sort = agg.sort
            when (sort?.type) {
                Agg.Sort.Type.Count -> {
                    if (sort.direction == Direction.Descending) {
                        buckets.sortedByDescending { it.count }
                    } else {
                        buckets.sortedBy { it.count }
                    }
                }
                Agg.Sort.Type.Lexical -> {
                    if (sort.direction == Direction.Descending) {
                        buckets.sortedByDescending { it.key.toString() }
                    } else {
                        buckets.sortedBy { it.key.toString() }
                    }
                }
                Agg.Sort.Type.None -> {
                    buckets.toList()
                }
                else -> buckets
            }
        }

        return result.copy(key = agg.key, buckets = buckets)
    }

    companion object {

        val aggXform = object : AggTransformer() {

            override fun transformXformAgg(agg: XformAgg, context: ExprContext): Agg {
                return ExtensionsTransformer(agg.extensions).transform(agg.agg, context)
            }

        }
    }

}
