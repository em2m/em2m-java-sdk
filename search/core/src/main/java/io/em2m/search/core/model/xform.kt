package io.em2m.search.core.model

import io.em2m.simplex.model.ExprContext

interface Transformer<T> {
    fun transformRequest(request: SearchRequest, context: ExprContext): SearchRequest = request
    fun transformResult(request: SearchRequest, result: SearchResult<T>, context: ExprContext): SearchResult<T> = result
    fun transformQuery(query: Query?, context: ExprContext): Query? = query
    fun transformItem(item: T, context: ExprContext): T = item
}

open class QueryTransformer {

    fun transform(query: Query, context: ExprContext): Query = when (query) {
        is AndQuery -> {
            transformAndQuery(query, context)
        }
        is OrQuery -> {
            transformOrQuery(query, context)
        }
        is NotQuery -> {
            transformNotQuery(query, context)
        }
        is TermQuery -> {
            transformTermQuery(query, context)
        }
        is TermsQuery -> {
            transformTermsQuery(query, context)
        }
        is MatchQuery -> {
            transformMatchQuery(query, context)
        }
        is PhraseQuery -> {
            transformPhraseQuery(query, context)
        }
        is PrefixQuery -> {
            transformPrefixQuery(query, context)
        }
        is WildcardQuery -> {
            transformWildcardQuery(query, context)
        }
        is RegexQuery -> {
            transformRegexQuery(query, context)
        }
        is RangeQuery -> {
            transformRangeQuery(query, context)
        }
        is DateRangeQuery -> {
            transformDateRangeQuery(query, context)
        }
        is BboxQuery -> {
            transformBboxQuery(query, context)
        }
        is MatchAllQuery -> {
            transformMatchAllQuery(query, context)
        }
        is NativeQuery -> {
            transformNativeQuery(query, context)
        }
        is NamedQuery -> {
            transformNamedQuery(query, context)
        }
        is LuceneQuery -> {
            transformLuceneQuery(query, context)
        }
        is ExistsQuery -> {
            transformExistsQuery(query, context)
        }
        else -> {
            query
        }
    }

    // Boolean Queries
    open fun transformAndQuery(query: AndQuery, context: ExprContext): Query {
        return AndQuery(query.of.mapNotNull { transform(it, context) })
    }

    open fun transformOrQuery(query: OrQuery, context: ExprContext): Query {
        return OrQuery(query.of.mapNotNull { transform(it, context) })
    }

    open fun transformNotQuery(query: NotQuery, context: ExprContext): Query {
        return NotQuery(query.of.mapNotNull { transform(it, context) })
    }

    // Field Queries

    open fun transformTermQuery(query: TermQuery, context: ExprContext): Query = query
    open fun transformTermsQuery(query: TermsQuery, context: ExprContext): Query = query
    open fun transformMatchQuery(query: MatchQuery, context: ExprContext): Query = query
    open fun transformPhraseQuery(query: PhraseQuery, context: ExprContext): Query = query
    open fun transformPrefixQuery(query: PrefixQuery, context: ExprContext): Query = query
    open fun transformWildcardQuery(query: WildcardQuery, context: ExprContext): Query = query
    open fun transformRegexQuery(query: RegexQuery, context: ExprContext): Query = query
    open fun transformDateRangeQuery(query: DateRangeQuery, context: ExprContext): Query = query
    open fun transformRangeQuery(query: RangeQuery, context: ExprContext): Query = query
    open fun transformBboxQuery(query: BboxQuery, context: ExprContext): Query = query
    open fun transformExistsQuery(query: ExistsQuery, context: ExprContext): Query = query

    // Special Queries

    open fun transformLuceneQuery(query: LuceneQuery, context: ExprContext): Query = query
    open fun transformMatchAllQuery(query: MatchAllQuery, context: ExprContext): Query = query
    open fun transformNativeQuery(query: NativeQuery, context: ExprContext): Query = query
    open fun transformNamedQuery(query: NamedQuery, context: ExprContext): Query = query

}

open class AggTransformer {

    fun transform(agg: Agg, context: ExprContext): Agg = when (agg) {

        is CardinalityAgg -> {
            transformCardinalityAgg(agg, context)
        }
        is DateHistogramAgg -> {
            transformDateHistogramAgg(agg, context)
        }
        is DateRangeAgg -> {
            transformDateRangeAgg(agg, context)
        }
        is FiltersAgg -> {
            transformFiltersAgg(agg, context)
        }
        is GeoBoundsAgg -> {
            transformGeoBoundsAgg(agg, context)
        }
        is GeoCentroidAgg -> {
            transformGeoCentroidAgg(agg, context)
        }
        is GeoDistanceAgg -> {
            transformGeoDistanceAgg(agg, context)
        }
        is GeoHashAgg -> {
            transformGeoHashAgg(agg, context)
        }
        is HistogramAgg -> {
            transformHistogramAgg(agg, context)
        }
        is MissingAgg -> {
            transformMissingAgg(agg, context)
        }
        is NamedAgg -> {
            transformNamedAgg(agg, context)
        }
        is NativeAgg -> {
            transformNativeAgg(agg, context)
        }
        is RangeAgg -> {
            transformRangeAgg(agg, context)
        }
        is StatsAgg -> {
            transformStatsAgg(agg, context)
        }
        is TermsAgg -> {
            transformTermsAgg(agg, context)
        }
        is XformAgg -> {
            transformXformAgg(agg, context)
        }
        else -> {
            agg
        }
    }

    open fun transformCardinalityAgg(agg: CardinalityAgg, context: ExprContext): Agg = agg
    open fun transformDateHistogramAgg(agg: DateHistogramAgg, context: ExprContext): Agg = agg
    open fun transformDateRangeAgg(agg: DateRangeAgg, context: ExprContext): Agg = agg
    open fun transformFiltersAgg(agg: FiltersAgg, context: ExprContext): Agg = agg
    open fun transformGeoBoundsAgg(agg: GeoBoundsAgg, context: ExprContext): Agg = agg
    open fun transformGeoCentroidAgg(agg: GeoCentroidAgg, context: ExprContext): Agg = agg
    open fun transformGeoDistanceAgg(agg: GeoDistanceAgg, context: ExprContext): Agg = agg
    open fun transformGeoHashAgg(agg: GeoHashAgg, context: ExprContext): Agg = agg
    open fun transformHistogramAgg(agg: HistogramAgg, context: ExprContext): Agg = agg
    open fun transformMissingAgg(agg: MissingAgg, context: ExprContext): Agg = agg
    open fun transformNamedAgg(agg: NamedAgg, context: ExprContext): Agg = agg
    open fun transformNativeAgg(agg: NativeAgg, context: ExprContext): Agg = agg
    open fun transformRangeAgg(agg: RangeAgg, context: ExprContext): Agg = agg
    open fun transformStatsAgg(agg: StatsAgg, context: ExprContext): Agg = agg
    open fun transformTermsAgg(agg: TermsAgg, context: ExprContext): Agg = agg
    open fun transformXformAgg(agg: XformAgg, context: ExprContext): Agg = agg
}

open class AggResultTransformer {

    open fun transform(aggResult: AggResult): AggResult {
        val buckets = aggResult.buckets?.map { transformBucket(it) }
        val stats = transformStats(aggResult.stats)
        val value = transformValue(aggResult.value)
        return AggResult(aggResult.key, buckets, stats, value, aggResult.op, aggResult.field, aggResult.type)
    }

    open fun transformBucket(bucket: Bucket): Bucket {
        return bucket
    }

    open fun transformStats(stats: Stats?): Stats? {
        return stats
    }

    open fun transformValue(value: Any?): Any? {
        return value
    }

}

class QueryTransformerAdapter<T>(val xforms: List<QueryTransformer>) : Transformer<T> {

    override fun transformQuery(query: Query?, context: ExprContext): Query? {
        return query?.let { xforms.fold(query) { current, xform -> xform.transform(current, context) } }
    }
}
