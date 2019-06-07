package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.utils.coerce

class NamedAggQueryTransformer(private val namedAggs: Map<String, Agg>, val timeZone: String?) : QueryTransformer() {

    override fun transformNamedQuery(query: NamedQuery): Query {
        val agg = namedAggs[query.name]
        val value = query.value

        return when (agg) {
            is TermsAgg ->
                TermQuery(agg.field, query.value)
            is RangeAgg -> {
                val range = requireNotNull(agg.ranges.find { it.key == query.value?.toString() })
                RangeQuery(agg.field, gte = range.from, lt = range.to)
            }
            is FiltersAgg -> {
                requireNotNull(agg.filters[query.value.toString()])
            }
            is DateRangeAgg -> {
                val range = requireNotNull(agg.ranges.find { it.key == query.value?.toString() })
                RangeQuery(agg.field, gte = range.from, lte = range.to, timeZone = timeZone)
            }
            is GeoBoundsAgg -> {
                TermQuery(agg.field, value)
            }
            is GeoCentroidAgg -> {
                TermQuery(agg.field, value)
            }
            is GeoHashAgg -> {
                TermQuery(agg.field + ".geohash", value)
            }
            is HistogramAgg -> {
                val gte: Double = requireNotNull(value.coerce())
                val lt: Double = gte + agg.interval
                RangeQuery(agg.field, gte, lt)
            }
            is DateHistogramAgg -> {
                TermQuery(agg.field, value)
            }
            is MissingAgg -> {
                TermQuery(agg.field, value)
            }
            is NativeAgg -> {
                throw UnsupportedOperationException()
            }
            is StatsAgg -> {
                TermQuery(agg.field, value)
            }
            else -> {
                super.transformNamedQuery(query)
            }
        }
    }

}