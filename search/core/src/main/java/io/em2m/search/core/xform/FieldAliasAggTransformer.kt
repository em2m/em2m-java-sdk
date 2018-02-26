package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class FieldAliasAggTransformer(val aliases: Map<String, Field>) : AggTransformer() {

    val queryTransformer = FieldAliasQueryTransformer(aliases)

    fun applyAlias(field: String): String {
        return aliases.getOrElse(field, { null })?.name ?: field
    }

    override fun transformDateHistogramAgg(agg: DateHistogramAgg) = DateHistogramAgg(applyAlias(agg.field), agg.format, agg.interval, agg.offset, agg.timeZone, agg.key, agg.extensions)
    override fun transformDateRangeAgg(agg: DateRangeAgg) = DateRangeAgg(applyAlias(agg.field), agg.format, agg.timeZone, agg.ranges, agg.key, agg.extensions)
    override fun transformFiltersAgg(agg: FiltersAgg) = FiltersAgg(agg.filters.mapValues { queryTransformer.transform(it.value) }, agg.key, agg.extensions)
    override fun transformGeoBoundsAgg(agg: GeoBoundsAgg) = GeoBoundsAgg(applyAlias(agg.field), agg.key, agg.extensions)
    override fun transformGeoCentroidAgg(agg: GeoCentroidAgg) = GeoCentroidAgg(applyAlias(agg.field), agg.key, agg.extensions)
    override fun transformGeoDistanceAgg(agg: GeoDistanceAgg) = GeoDistanceAgg(applyAlias(agg.field), agg.origin, agg.unit, agg.ranges, agg.key, agg.extensions)
    override fun transformGeoHashAgg(agg: GeoHashAgg) = GeoHashAgg(applyAlias(agg.field), agg.precision, agg.size, agg.key, agg.extensions)
    override fun transformHistogramAgg(agg: HistogramAgg) = HistogramAgg(applyAlias(agg.field), agg.interval, agg.offset, agg.key, agg.extensions)
    override fun transformMissingAgg(agg: MissingAgg) = MissingAgg(applyAlias(agg.field), agg.key, agg.extensions)
    override fun transformRangeAgg(agg: RangeAgg) = RangeAgg(applyAlias(agg.field), agg.ranges, agg.key, agg.extensions)
    override fun transformStatsAgg(agg: StatsAgg) = StatsAgg(applyAlias(agg.field), agg.key, agg.extensions)
    override fun transformTermsAgg(agg: TermsAgg) = TermsAgg(applyAlias(agg.field), agg.size, agg.key, agg.sort, agg.field, agg.missing, agg.extensions)
}