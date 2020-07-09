package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class NamedAggTransformer(val namedAggs: Map<String, Agg>) : AggTransformer() {

    override fun transformNamedAgg(agg: NamedAgg): Agg {

        return namedAggs[agg.name]
                ?.let { KeyAggTransformer(agg.key).transform(it) }
                ?.let { ExtensionsTransformer(agg.extensions).transform(it) } ?: agg
    }


    class KeyAggTransformer(val key: String) : AggTransformer() {

        override fun transformDateHistogramAgg(agg: DateHistogramAgg) = DateHistogramAgg(agg.field, agg.format, agg.interval, agg.offset, agg.timeZone, agg.missing, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformDateRangeAgg(agg: DateRangeAgg) = DateRangeAgg(agg.field, agg.format, agg.timeZone, agg.ranges, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformFiltersAgg(agg: FiltersAgg) = FiltersAgg(agg.filters, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoBoundsAgg(agg: GeoBoundsAgg) = GeoBoundsAgg(agg.field, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoCentroidAgg(agg: GeoCentroidAgg) = GeoCentroidAgg(agg.field, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoDistanceAgg(agg: GeoDistanceAgg) = GeoDistanceAgg(agg.field, agg.origin, agg.unit, agg.ranges, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoHashAgg(agg: GeoHashAgg) = GeoHashAgg(agg.field, agg.precision, agg.size, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformHistogramAgg(agg: HistogramAgg) = HistogramAgg(agg.field, agg.format, agg.interval, agg.offset, key, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformMissingAgg(agg: MissingAgg) = MissingAgg(agg.field, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformRangeAgg(agg: RangeAgg) = RangeAgg(agg.field, agg.ranges, key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformStatsAgg(agg: StatsAgg) = StatsAgg(agg.field, key, agg.format, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformTermsAgg(agg: TermsAgg) = TermsAgg(agg.field, agg.size, key, agg.sort, agg.format, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformXformAgg(agg: XformAgg) = XformAgg(key, agg.sort, agg.agg, agg.extensions, agg.minDocCount, agg.bucket)
    }

}