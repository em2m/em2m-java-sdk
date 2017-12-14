package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class KeyAggTransformer(val key: String) : AggTransformer() {


    override fun transformDateHistogramAgg(agg: DateHistogramAgg) = DateHistogramAgg(agg.field, agg.format, agg.interval, agg.offset, agg.timeZone, key)
    override fun transformDateRangeAgg(agg: DateRangeAgg) = DateRangeAgg(agg.field, agg.format, agg.timeZone, agg.ranges, key)
    override fun transformFiltersAgg(agg: FiltersAgg) = FiltersAgg(agg.filters, key)
    override fun transformGeoBoundsAgg(agg: GeoBoundsAgg) = GeoBoundsAgg(agg.field, key)
    override fun transformGeoCentroidAgg(agg: GeoCentroidAgg) = GeoCentroidAgg(agg.field, key)
    override fun transformGeoDistanceAgg(agg: GeoDistanceAgg) = GeoDistanceAgg(agg.field, agg.origin, agg.unit, agg.ranges, key)
    override fun transformGeoHashAgg(agg: GeoHashAgg) = GeoHashAgg(agg.field, agg.precision, agg.size, key)
    override fun transformHistogramAgg(agg: HistogramAgg) = HistogramAgg(agg.field, agg.interval, agg.offset, key)
    override fun transformMissingAgg(agg: MissingAgg) = MissingAgg(agg.field, key)
    override fun transformRangeAgg(agg: RangeAgg) = RangeAgg(agg.field, agg.ranges, key)
    override fun transformStatsAgg(agg: StatsAgg) = StatsAgg(agg.field, key)
    override fun transformTermsAgg(agg: TermsAgg) = TermsAgg(agg.field, agg.size, key, agg.sort, agg.format, agg.missing)
}