package io.em2m.search.core.xform

import io.em2m.search.core.model.*

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
}