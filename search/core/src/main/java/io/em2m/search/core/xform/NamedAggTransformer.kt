package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.simplex.model.ExprContext

class NamedAggTransformer(val namedAggs: Map<String, Agg>) : AggTransformer() {

    override fun transformNamedAgg(agg: NamedAgg, context: ExprContext): Agg {

        return namedAggs[agg.name]
                ?.let { KeyAggTransformer(agg.key).transform(it, context) }
                ?.let { ExtensionsTransformer(agg.extensions).transform(it, context) } ?: agg
    }


    class KeyAggTransformer(val key: String) : AggTransformer() {

        override fun transformDateHistogramAgg(agg: DateHistogramAgg, context: ExprContext) = DateHistogramAgg(agg.field, agg.format, agg.interval, agg.offset, agg.timeZone, agg.missing, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformDateRangeAgg(agg: DateRangeAgg, context: ExprContext) = DateRangeAgg(agg.field, agg.format, agg.timeZone, agg.ranges, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformFiltersAgg(agg: FiltersAgg, context: ExprContext) = FiltersAgg(agg.filters, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoBoundsAgg(agg: GeoBoundsAgg, context: ExprContext) = GeoBoundsAgg(agg.field, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoCentroidAgg(agg: GeoCentroidAgg, context: ExprContext) = GeoCentroidAgg(agg.field, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoDistanceAgg(agg: GeoDistanceAgg, context: ExprContext) = GeoDistanceAgg(agg.field, agg.origin, agg.unit, agg.ranges, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoHashAgg(agg: GeoHashAgg, context: ExprContext) = GeoHashAgg(agg.field, agg.precision, agg.size, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformHistogramAgg(agg: HistogramAgg, context: ExprContext) = HistogramAgg(agg.field, agg.format, agg.interval, agg.offset, key, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformMissingAgg(agg: MissingAgg, context: ExprContext) = MissingAgg(agg.field, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformRangeAgg(agg: RangeAgg, context: ExprContext) = RangeAgg(agg.field, agg.ranges, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformStatsAgg(agg: StatsAgg, context: ExprContext) = StatsAgg(agg.field, key, agg.format, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformTermsAgg(agg: TermsAgg, context: ExprContext) = TermsAgg(agg.field, agg.size, key, agg.sort, agg.format, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformXformAgg(agg: XformAgg, context: ExprContext) = XformAgg(key, agg.sort, agg.agg, agg.extensions, agg.minDocCount, agg.bucket)
    }

}
