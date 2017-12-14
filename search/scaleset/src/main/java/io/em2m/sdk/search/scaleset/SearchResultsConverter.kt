package io.em2m.sdk.search.scaleset

import com.scaleset.search.AggregationResults
import com.scaleset.search.Results
import io.em2m.search.core.model.AggResult
import io.em2m.search.core.model.Bucket
import io.em2m.search.core.model.SearchResult
import io.em2m.search.core.model.Stats
import com.scaleset.search.Bucket as ScalesetBucket
import com.scaleset.search.Stats as ScalesetStats

class SearchResultsConverter<T> {

    fun convertSearchResult(result: SearchResult<T>): Results<T> {
        val aggs = result.aggs.map { convertAgg(it.value) }.associate { it.name to it }
        val items = result.items
        val totalItems = result.totalItems.toInt()
        val headers = result.headers
        val fields = result.fields.map { it.name }

        return Results(null, aggs, items, totalItems, null, headers, fields)
    }

    fun convertAgg(agg: AggResult): AggregationResults {
        val buckets = agg.buckets?.map { it.toScaleset() }
        return AggregationResults(agg.key, buckets, agg.stats?.toScaleset())
    }

    fun Bucket.toScaleset(): ScalesetBucket {
        val result = ScalesetBucket(key, count, label, stats?.toScaleset())
        // todo: nested aggs?
        return result
    }

    fun Stats.toScaleset(): ScalesetStats {
        return ScalesetStats(count, sum, min, max, avg)
    }
}