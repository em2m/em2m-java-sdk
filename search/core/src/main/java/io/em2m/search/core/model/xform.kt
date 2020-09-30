package io.em2m.search.core.model

interface Transformer<T> {
    fun transformRequest(request: SearchRequest): SearchRequest = request
    fun transformResult(request: SearchRequest, result: SearchResult<T>): SearchResult<T> = result
    fun transformQuery(query: Query?): Query? = query
    fun transformItem(item: T): T = item
}

open class QueryTransformer {

    fun transform(query: Query): Query = when (query) {
        is AndQuery -> {
            transformAndQuery(query)
        }
        is OrQuery -> {
            transformOrQuery(query)
        }
        is NotQuery -> {
            transformNotQuery(query)
        }
        is TermQuery -> {
            transformTermQuery(query)
        }
        is TermsQuery -> {
            transformTermsQuery(query)
        }
        is MatchQuery -> {
            transformMatchQuery(query)
        }
        is PhraseQuery -> {
            transformPhraseQuery(query)
        }
        is PrefixQuery -> {
            transformPrefixQuery(query)
        }
        is WildcardQuery -> {
            transformWildcardQuery(query)
        }
        is RegexQuery -> {
            transformRegexQuery(query)
        }
        is RangeQuery -> {
            transformRangeQuery(query)
        }
        is DateRangeQuery -> {
            transformDateRangeQuery(query)
        }
        is BboxQuery -> {
            transformBboxQuery(query)
        }
        is MatchAllQuery -> {
            transformMatchAllQuery(query)
        }
        is NativeQuery -> {
            transformNativeQuery(query)
        }
        is NamedQuery -> {
            transformNamedQuery(query)
        }
        is LuceneQuery -> {
            transformLuceneQuery(query)
        }
        is ExistsQuery -> {
            transformExistsQuery(query)
        }
        else -> {
            query
        }
    }

    // Boolean Queries
    open fun transformAndQuery(query: AndQuery): Query {
        return AndQuery(query.of.mapNotNull { transform(it) })
    }

    open fun transformOrQuery(query: OrQuery): Query {
        return OrQuery(query.of.mapNotNull { transform(it) })
    }

    open fun transformNotQuery(query: NotQuery): Query {
        return NotQuery(query.of.mapNotNull { transform(it) })
    }

    // Field Queries

    open fun transformTermQuery(query: TermQuery): Query = query
    open fun transformTermsQuery(query: TermsQuery): Query = query
    open fun transformMatchQuery(query: MatchQuery): Query = query
    open fun transformPhraseQuery(query: PhraseQuery): Query = query
    open fun transformPrefixQuery(query: PrefixQuery): Query = query
    open fun transformWildcardQuery(query: WildcardQuery): Query = query
    open fun transformRegexQuery(query: RegexQuery): Query = query
    open fun transformDateRangeQuery(query: DateRangeQuery): Query = query
    open fun transformRangeQuery(query: RangeQuery): Query = query
    open fun transformBboxQuery(query: BboxQuery): Query = query
    open fun transformExistsQuery(query: ExistsQuery): Query = query

    // Special Queries

    open fun transformLuceneQuery(query: LuceneQuery): Query = query
    open fun transformMatchAllQuery(query: MatchAllQuery): Query = query
    open fun transformNativeQuery(query: NativeQuery): Query = query
    open fun transformNamedQuery(query: NamedQuery): Query = query


}

open class AggTransformer {

    fun transform(agg: Agg): Agg = when (agg) {

        is DateHistogramAgg -> {
            transformDateHistogramAgg(agg)
        }
        is DateRangeAgg -> {
            transformDateRangeAgg(agg)
        }
        is FiltersAgg -> {
            transformFiltersAgg(agg)
        }
        is GeoBoundsAgg -> {
            transformGeoBoundsAgg(agg)
        }
        is GeoCentroidAgg -> {
            transformGeoCentroidAgg(agg)
        }
        is GeoDistanceAgg -> {
            transformGeoDistanceAgg(agg)
        }
        is GeoHashAgg -> {
            transformGeoHashAgg(agg)
        }
        is HistogramAgg -> {
            transformHistogramAgg(agg)
        }
        is MissingAgg -> {
            transformMissingAgg(agg)
        }
        is NamedAgg -> {
            transformNamedAgg(agg)
        }
        is NativeAgg -> {
            transformNativeAgg(agg)
        }
        is RangeAgg -> {
            transformRangeAgg(agg)
        }
        is StatsAgg -> {
            transformStatsAgg(agg)
        }
        is TermsAgg -> {
            transformTermsAgg(agg)
        }
        is XformAgg -> {
            transformXformAgg(agg)
        }
        else -> {
            agg
        }
    }

    open fun transformDateHistogramAgg(agg: DateHistogramAgg): Agg = agg
    open fun transformDateRangeAgg(agg: DateRangeAgg): Agg = agg
    open fun transformFiltersAgg(agg: FiltersAgg): Agg = agg
    open fun transformGeoBoundsAgg(agg: GeoBoundsAgg): Agg = agg
    open fun transformGeoCentroidAgg(agg: GeoCentroidAgg): Agg = agg
    open fun transformGeoDistanceAgg(agg: GeoDistanceAgg): Agg = agg
    open fun transformGeoHashAgg(agg: GeoHashAgg): Agg = agg
    open fun transformHistogramAgg(agg: HistogramAgg): Agg = agg
    open fun transformMissingAgg(agg: MissingAgg): Agg = agg
    open fun transformNamedAgg(agg: NamedAgg): Agg = agg
    open fun transformNativeAgg(agg: NativeAgg): Agg = agg
    open fun transformRangeAgg(agg: RangeAgg): Agg = agg
    open fun transformStatsAgg(agg: StatsAgg): Agg = agg
    open fun transformTermsAgg(agg: TermsAgg): Agg = agg
    open fun transformXformAgg(agg: XformAgg): Agg = agg
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

    override fun transformQuery(query: Query?): Query? {
        return query?.let { xforms.fold(query) { current, xform -> xform.transform(current) } }
    }
}
